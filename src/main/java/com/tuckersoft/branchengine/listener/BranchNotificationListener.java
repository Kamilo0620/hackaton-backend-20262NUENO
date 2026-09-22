package com.tuckersoft.branchengine.listener;

import com.tuckersoft.branchengine.event.DecisionCommittedEvent;
import com.tuckersoft.branchengine.model.Decision;
import com.tuckersoft.branchengine.model.RealityLog;
import com.tuckersoft.branchengine.repository.DecisionRepository;
import com.tuckersoft.branchengine.repository.RealityLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class BranchNotificationListener {
    private final DecisionRepository decisionRepository;
    private final RealityLogRepository realityLogRepository;
    private final JavaMailSender mailSender;

    @Async("branchExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alCommit(DecisionCommittedEvent evento) {
        Decision decision = decisionRepository.findById(evento.decisionId()).orElse(null);
        if (decision == null) {
            return;
        }

        decision.setStatus("PROCESANDO");
        decision.setUpdatedAt(Instant.now());
        decisionRepository.saveAndFlush(decision);

        String subject = String.format("[TUCKERSOFT] %s en %s | Impacto %s",
                evento.branchType(), evento.playerTag(), evento.impactLevel());

        String ending = evento.endingCode() != null ? evento.endingCode() : "-";
        String resolvedNode = evento.resolvedNodeCode() != null ? evento.resolvedNodeCode() : "-";

        String body = String.format("""
                Hola %s,

                Una partida de prueba acaba de ramificarse.

                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                Decision ID      : #%d
                Jugador          : %s
                Rama             : %s
                Impacto          : %s
                Departamento     : %s
                Consecuencia     : %s
                Nodo origen      : %s
                Nodo destino     : %s
                Estado partida   : %s
                Lucidez          : %d/100
                Nivel de control : %d/100
                Final            : %s
                Registrada       : %s
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

                Decisión original del jugador:
                "%s"

                — Tuckersoft Branch Engine, 1984
                """,
                evento.displayName(),
                evento.decisionId(),
                evento.playerTag(),
                evento.branchType(),
                evento.impactLevel(),
                evento.handlerUnit(),
                evento.outcomeCode(),
                evento.sourceNodeCode(),
                resolvedNode,
                evento.playthroughStatus(),
                evento.lucidity(),
                evento.controlLevel(),
                ending,
                evento.createdAt().toString(),
                evento.rawInput()
        );

        String finalStatus;
        try {
            if ("MAIL_FAILURE".equals(evento.simulateHeader())) {
                throw new RuntimeException("Simulated mail failure via X-Bandersnatch-Simulate");
            }

            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(evento.recipientEmail());
            mail.setSubject(subject);
            mail.setText(body);
            mailSender.send(mail);

            finalStatus = "ESTABILIZADA";
            decision.setStatus(finalStatus);
            decision.setUpdatedAt(Instant.now());
            decisionRepository.save(decision);

            RealityLog logEntity = RealityLog.builder()
                    .decision(decision)
                    .recipientEmail(evento.recipientEmail())
                    .subject(subject)
                    .logStatus("SENT")
                    .errorMessage(null)
                    .sentAt(Instant.now())
                    .createdAt(Instant.now())
                    .build();
            realityLogRepository.save(logEntity);
        } catch (Exception ex) {
            finalStatus = "ERROR";
            decision.setStatus(finalStatus);
            decision.setUpdatedAt(Instant.now());
            decisionRepository.save(decision);

            RealityLog logEntity = RealityLog.builder()
                    .decision(decision)
                    .recipientEmail(evento.recipientEmail())
                    .subject(subject)
                    .logStatus("FAILED")
                    .errorMessage(ex.getMessage())
                    .sentAt(null)
                    .createdAt(Instant.now())
                    .build();
            realityLogRepository.save(logEntity);
            log.error("Error al despachar el informe de realidad: {}", ex.getMessage());
        }

        System.out.printf(
                "[BRANCH-LOG] Decision ID: %d | Player: %s | Branch: %s | Impact: %s | Unit: %s | Node: %s -> %s | Thread: %s | Status: %s%n",
                evento.decisionId(),
                evento.playerTag(),
                evento.branchType(),
                evento.impactLevel(),
                evento.handlerUnit(),
                evento.sourceNodeCode(),
                evento.resolvedNodeCode(),
                Thread.currentThread().getName(),
                finalStatus
        );
    }
}
