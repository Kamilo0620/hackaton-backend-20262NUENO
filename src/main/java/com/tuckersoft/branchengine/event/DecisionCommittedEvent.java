package com.tuckersoft.branchengine.event;

import java.time.Instant;

public record DecisionCommittedEvent(
    Long decisionId,
    String playerTag,
    String branchType,
    String impactLevel,
    String handlerUnit,
    String outcomeCode,
    String sourceNodeCode,
    String resolvedNodeCode,
    String playthroughStatus,
    Integer lucidity,
    Integer controlLevel,
    String endingCode,
    Instant createdAt,
    String rawInput,
    String recipientEmail,
    String displayName,
    String simulateHeader
) {}
