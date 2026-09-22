package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.dto.Dtos.*;
import com.tuckersoft.branchengine.exception.BusinessException;
import com.tuckersoft.branchengine.exception.ConflictException;
import com.tuckersoft.branchengine.exception.ResourceNotFoundException;
import com.tuckersoft.branchengine.model.*;
import com.tuckersoft.branchengine.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaythroughService {
    private final PlaythroughRepository playthroughRepository;
    private final StoryNodeRepository nodeRepository;
    private final UserRepository userRepository;
    private final DecisionRepository decisionRepository;

    @Transactional
    public PlaythroughResponse create(PlaythroughRequest req, String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElseThrow();

        if (playthroughRepository.existsByPlayerTag(req.playerTag())) {
            throw new ConflictException("playerTag ya registrado");
        }

        StoryNode startNode = nodeRepository.findByNodeCode(req.startNodeCode())
                .orElseThrow(() -> new ResourceNotFoundException("startNodeCode no existe"));

        if (startNode.getCurrentBranches() >= startNode.getBranchCapacity()) {
            throw new BusinessException("El nodo inicial ha alcanzado su capacidad máxima");
        }

        startNode.setCurrentBranches(startNode.getCurrentBranches() + 1);
        nodeRepository.save(startNode);

        Instant now = Instant.now();
        Playthrough p = Playthrough.builder()
                .playerTag(req.playerTag())
                .user(user)
                .startNodeCode(startNode.getNodeCode())
                .currentNode(startNode)
                .lucidity(100)
                .controlLevel(0)
                .status("ACTIVA")
                .endingCode(null)
                .createdAt(now)
                .updatedAt(now)
                .build();
        playthroughRepository.save(p);

        return toDto(p);
    }

    public List<PlaythroughResponse> getAll(String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElseThrow();
        List<Playthrough> list = "ROLE_ADMIN".equals(user.getRole())
                ? playthroughRepository.findAllByOrderByCreatedAtDesc()
                : playthroughRepository.findByUserOrderByCreatedAtDesc(user);
        return list.stream().map(this::toDto).toList();
    }

    public PlaythroughResponse getById(Long id, String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElseThrow();
        Playthrough p = playthroughRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Partida no encontrada"));

        if (!"ROLE_ADMIN".equals(user.getRole()) && !p.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Acceso no permitido a esta partida");
        }

        return toDto(p);
    }

    public PlaythroughPathResponse getPath(Long id, String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElseThrow();
        Playthrough p = playthroughRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Partida no encontrada"));

        if (!"ROLE_ADMIN".equals(user.getRole()) && !p.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Acceso no permitido a esta partida");
        }

        List<Decision> decisions = decisionRepository.findByPlaythroughAndResolvedNodeCodeIsNotNullOrderByCreatedAtAsc(p);
        List<PathStepResponse> steps = new ArrayList<>();
        int order = 1;
        for (Decision d : decisions) {
            steps.add(new PathStepResponse(
                    order++,
                    d.getId(),
                    d.getNode().getNodeCode(),
                    d.getResolvedNodeCode(),
                    d.getBranchType(),
                    d.getImpactLevel(),
                    d.getCreatedAt()
            ));
        }

        return new PlaythroughPathResponse(
                p.getId(),
                p.getPlayerTag(),
                p.getStatus(),
                p.getEndingCode(),
                p.getStartNodeCode(),
                p.getCurrentNode().getNodeCode(),
                steps
        );
    }

    private PlaythroughResponse toDto(Playthrough p) {
        return new PlaythroughResponse(
                p.getId(), p.getPlayerTag(), p.getUser().getEmail(),
                p.getStartNodeCode(), p.getCurrentNode().getNodeCode(),
                p.getLucidity(), p.getControlLevel(), p.getStatus(),
                p.getEndingCode(), p.getCreatedAt(), p.getUpdatedAt()
        );
    }
}
