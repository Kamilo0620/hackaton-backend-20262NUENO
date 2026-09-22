package com.tuckersoft.branchengine;

import com.tuckersoft.branchengine.dto.Dtos.DecisionRequest;
import com.tuckersoft.branchengine.dto.Dtos.DecisionResponse;
import com.tuckersoft.branchengine.event.DecisionCommittedEvent;
import com.tuckersoft.branchengine.model.*;
import com.tuckersoft.branchengine.repository.*;
import com.tuckersoft.branchengine.service.DecisionClassifier;
import com.tuckersoft.branchengine.service.DecisionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DecisionServiceTest {

    @Mock
    private DecisionRepository decisionRepository;
    @Mock
    private PlaythroughRepository playthroughRepository;
    @Mock
    private StoryNodeRepository storyNodeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RealityLogRepository realityLogRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DecisionService decisionService;

    private User owner;
    private StoryNode node;
    private Playthrough playthrough;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).email("colin@tuckersoft.co.uk").displayName("Colin Ritman").role("ROLE_USER").createdAt(Instant.now()).build();
        node = StoryNode.builder().id(10L).nodeCode("NODE-CEREAL").title("Title").sceneText("Scene description").branchCapacity(5).currentBranches(1).primaryBranchCode("NODE-BUS").glitchBranchCode("NODE-ESPEJO").createdAt(Instant.now()).build();
        playthrough = Playthrough.builder().id(100L).playerTag("STEFAN-01").user(owner).startNodeCode("NODE-CEREAL").currentNode(node).lucidity(100).controlLevel(0).status("ACTIVA").endingCode(null).createdAt(Instant.now()).updatedAt(Instant.now()).build();
    }

    @Test
    @DisplayName("1. Stefan destruye la camara clasifica como RUPTURA_CUARTA_PARED")
    void testPrecedenceRuleFourthWall() {
        var res = DecisionClassifier.classify("Stefan destruye la camara");
        assertEquals("RUPTURA_CUARTA_PARED", res.branchType());
        assertEquals("Departamento Netflix", res.handlerUnit());
        assertEquals("BREAK_FOURTH_WALL", res.outcomeCode());
    }

    @Test
    @DisplayName("2. Entrada sin letras clasifica como ENTRADA_CORRUPTA y no modifica partida")
    void testCorruptedInputLeavesPlaythroughUntouched() {
        when(userRepository.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
        when(playthroughRepository.findById(100L)).thenReturn(Optional.of(playthrough));
        when(decisionRepository.save(any(Decision.class))).thenAnswer(i -> i.getArgument(0));

        DecisionRequest req = new DecisionRequest(100L, "1234567890!?", "LEVE");
        DecisionResponse resp = decisionService.createDecision(req, owner.getEmail(), null);

        assertEquals("ENTRADA_CORRUPTA", resp.branchType());
        assertEquals("ERROR", resp.status());
        assertNull(resp.resolvedNodeCode());
        assertEquals(100, playthrough.getLucidity());
        assertEquals(0, playthrough.getControlLevel());
        verify(playthroughRepository, never()).save(any(Playthrough.class));
    }

    @Test
    @DisplayName("3. Impacto CRITICO baja lucidez 40 y sube control 45 respetando limites")
    void testCriticalImpactStatsBounds() {
        when(userRepository.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
        when(playthroughRepository.findById(100L)).thenReturn(Optional.of(playthrough));
        when(storyNodeRepository.existsByNodeCode("NODE-ESPEJO")).thenReturn(true);
        when(storyNodeRepository.findByNodeCode("NODE-ESPEJO")).thenReturn(Optional.of(node));
        when(decisionRepository.save(any(Decision.class))).thenAnswer(i -> i.getArgument(0));

        DecisionRequest req = new DecisionRequest(100L, "Stefan acepta la oferta y programa", "CRITICO");
        DecisionResponse resp = decisionService.createDecision(req, owner.getEmail(), null);

        assertEquals(60, resp.lucidity());
        assertEquals(45, resp.controlLevel());
        assertEquals("NODE-ESPEJO", resp.resolvedNodeCode());
    }

    @Test
    @DisplayName("4. controlLevel >= 100 termina con ENDING_PAC_SYMBOL aun con lucidez en 0")
    void testPacSymbolPrecedenceOverWhiteBear() {
        playthrough.setLucidity(20);
        playthrough.setControlLevel(90);

        when(userRepository.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
        when(playthroughRepository.findById(100L)).thenReturn(Optional.of(playthrough));
        when(decisionRepository.save(any(Decision.class))).thenAnswer(i -> i.getArgument(0));

        DecisionRequest req = new DecisionRequest(100L, "Stefan acepta la oferta y programa", "CRITICO");
        DecisionResponse resp = decisionService.createDecision(req, owner.getEmail(), null);

        assertEquals(0, resp.lucidity());
        assertEquals(100, resp.controlLevel());
        assertEquals("FINALIZADA", resp.playthroughStatus());
        assertEquals("ENDING_PAC_SYMBOL", resp.endingCode());
    }

    @Test
    @DisplayName("5. publishEvent se ejecuta 1 vez en normal y 0 veces en ENTRADA_CORRUPTA")
    void testEventPublishingBehavior() {
        when(userRepository.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
        when(playthroughRepository.findById(100L)).thenReturn(Optional.of(playthrough));
        when(storyNodeRepository.existsByNodeCode("NODE-BUS")).thenReturn(true);
        when(storyNodeRepository.findByNodeCode("NODE-BUS")).thenReturn(Optional.of(node));
        when(decisionRepository.save(any(Decision.class))).thenAnswer(i -> i.getArgument(0));

        DecisionRequest reqNormal = new DecisionRequest(100L, "Stefan toma el autobus a la oficina", "LEVE");
        decisionService.createDecision(reqNormal, owner.getEmail(), null);
        verify(eventPublisher, times(1)).publishEvent(any(DecisionCommittedEvent.class));

        DecisionRequest reqCorrupt = new DecisionRequest(100L, "1234567890!?", "LEVE");
        decisionService.createDecision(reqCorrupt, owner.getEmail(), null);
        verify(eventPublisher, times(1)).publishEvent(any(DecisionCommittedEvent.class));
    }
}
