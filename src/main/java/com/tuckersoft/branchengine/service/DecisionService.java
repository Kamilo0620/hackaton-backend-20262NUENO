package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.dto.Dtos.*;
import com.tuckersoft.branchengine.event.DecisionCommittedEvent;
import com.tuckersoft.branchengine.exception.*;
import com.tuckersoft.branchengine.model.*;
import com.tuckersoft.branchengine.repository.*;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DecisionService {
    private final DecisionRepository decisionRepository;
    private final PlaythroughRepository playthroughRepository;
    private final StoryNodeRepository storyNodeRepository;
    private final UserRepository userRepository;
    private final RealityLogRepository realityLogRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public DecisionResponse createDecision(DecisionRequest req, String userEmail, String simulateHeader) {
        User user = userRepository.findByEmail(userEmail).orElseThrow();
        Playthrough p = playthroughRepository.findById(req.playthroughId())
                .orElseThrow(() -> new ResourceNotFoundException("Partida no encontrada"));

        if (!p.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("No puedes decidir sobre una partida ajena");
        }

        if ("FINALIZADA".equals(p.getStatus())) {
            throw new ConflictException("La partida ya está FINALIZADA");
        }

        if (!List.of("LEVE", "MODERADO", "GRAVE", "CRITICO").contains(req.impactLevel())) {
            throw new BusinessException("impactLevel inválido");
        }

        var classification = DecisionClassifier.classify(req.rawInput());
        StoryNode sourceNode = p.getCurrentNode();
        Instant now = Instant.now();

        if ("ENTRADA_CORRUPTA".equals(classification.branchType())) {
            Decision corrupted = Decision.builder()
                    .playthrough(p)
                    .node(sourceNode)
                    .rawInput(req.rawInput())
                    .branchType(classification.branchType())
                    .impactLevel(req.impactLevel())
                    .handlerUnit(classification.handlerUnit())
                    .outcomeCode(classification.outcomeCode())
                    .resolvedNodeCode(null)
                    .status("ERROR")
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            decisionRepository.save(corrupted);
            return toDto(corrupted, p);
        }

        int deltaLucidity = switch (req.impactLevel()) {
            case "LEVE" -> -5;
            case "MODERADO" -> -15;
            case "GRAVE" -> -30;
            case "CRITICO" -> -40;
            default -> 0;
        };

        int deltaControl = switch (req.impactLevel()) {
            case "LEVE" -> 5;
            case "MODERADO" -> 10;
            case "GRAVE" -> 20;
            case "CRITICO" -> 45;
            default -> 0;
        };

        p.setLucidity(Math.max(0, Math.min(100, p.getLucidity() + deltaLucidity)));
        p.setControlLevel(Math.max(0, Math.min(100, p.getControlLevel() + deltaControl)));

        String resolvedCode;
        if ("RUPTURA_CUARTA_PARED".equals(classification.branchType()) || "CRITICO".equals(req.impactLevel())) {
            resolvedCode = sourceNode.getGlitchBranchCode();
        } else {
            resolvedCode = sourceNode.getPrimaryBranchCode();
        }

        if (p.getControlLevel() >= 100) {
            p.setStatus("FINALIZADA");
            p.setEndingCode("ENDING_PAC_SYMBOL");
        } else if (p.getLucidity() <= 0) {
            p.setStatus("FINALIZADA");
            p.setEndingCode("ENDING_WHITE_BEAR");
        } else if (resolvedCode == null || !storyNodeRepository.existsByNodeCode(resolvedCode)) {
            p.setStatus("FINALIZADA");
            p.setEndingCode("ENDING_NETFLIX_CUT");
        } else {
            p.setStatus("ACTIVA");
            p.setCurrentNode(storyNodeRepository.findByNodeCode(resolvedCode).orElseThrow());
        }

        p.setUpdatedAt(now);
        playthroughRepository.save(p);

        Decision decision = Decision.builder()
                .playthrough(p)
                .node(sourceNode)
                .rawInput(req.rawInput())
                .branchType(classification.branchType())
                .impactLevel(req.impactLevel())
                .handlerUnit(classification.handlerUnit())
                .outcomeCode(classification.outcomeCode())
                .resolvedNodeCode(resolvedCode)
                .status("REGISTRADA")
                .createdAt(now)
                .updatedAt(now)
                .build();
        decisionRepository.save(decision);

        eventPublisher.publishEvent(new DecisionCommittedEvent(
                decision.getId(),
                p.getPlayerTag(),
                classification.branchType(),
                req.impactLevel(),
                classification.handlerUnit(),
                classification.outcomeCode(),
                sourceNode.getNodeCode(),
                resolvedCode,
                p.getStatus(),
                p.getLucidity(),
                p.getControlLevel(),
                p.getEndingCode(),
                decision.getCreatedAt(),
                req.rawInput(),
                p.getUser().getEmail(),
                p.getUser().getDisplayName(),
                simulateHeader
        ));

        return toDto(decision, p);
    }

    public PagedResponse<DecisionResponse> getDecisions(
            String branchType, String impactLevel, String status, Long playthroughId,
            int page, int size, String userEmail
    ) {
        User user = userRepository.findByEmail(userEmail).orElseThrow();
        boolean isAdmin = "ROLE_ADMIN".equals(user.getRole());

        Specification<Decision> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!isAdmin) {
                predicates.add(cb.equal(root.get("playthrough").get("user"), user));
            }
            if (branchType != null && !branchType.isBlank()) {
                predicates.add(cb.equal(root.get("branchType"), branchType));
            }
            if (impactLevel != null && !impactLevel.isBlank()) {
                predicates.add(cb.equal(root.get("impactLevel"), impactLevel));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (playthroughId != null) {
                predicates.add(cb.equal(root.get("playthrough").get("id"), playthroughId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Decision> paged = decisionRepository.findAll(spec, pageable);

        List<DecisionResponse> content = paged.getContent().stream()
                .map(d -> toDto(d, d.getPlaythrough()))
                .toList();

        return new PagedResponse<>(content, paged.getTotalElements(), paged.getTotalPages(), page, size);
    }

    public DecisionResponse getById(Long id, String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElseThrow();
        Decision d = decisionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Decisión no encontrada"));

        if (!"ROLE_ADMIN".equals(user.getRole()) && !d.getPlaythrough().getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Acceso no permitido a esta decisión");
        }

        return toDto(d, d.getPlaythrough());
    }

    public List<RealityLogResponse> getRealityLogs(Long decisionId, String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElseThrow();
        Decision d = decisionRepository.findById(decisionId)
                .orElseThrow(() -> new ResourceNotFoundException("Decisión no encontrada"));

        if (!"ROLE_ADMIN".equals(user.getRole()) && !d.getPlaythrough().getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Acceso no permitido a los registros");
        }

        return realityLogRepository.findByDecisionOrderByCreatedAtAsc(d).stream()
                .map(l -> new RealityLogResponse(
                        l.getId(), d.getId(), l.getRecipientEmail(), l.getSubject(),
                        l.getLogStatus(), l.getErrorMessage(), l.getSentAt(), l.getCreatedAt()
                ))
                .toList();
    }

    private DecisionResponse toDto(Decision d, Playthrough p) {
        return new DecisionResponse(
                d.getId(), p.getId(), p.getPlayerTag(),
                d.getNode().getNodeCode(), d.getResolvedNodeCode(),
                d.getRawInput(), d.getBranchType(), d.getImpactLevel(),
                d.getHandlerUnit(), d.getOutcomeCode(), d.getStatus(),
                p.getStatus(), p.getLucidity(), p.getControlLevel(),
                p.getEndingCode(), d.getCreatedAt(), d.getUpdatedAt()
        );
    }
}
