package com.tuckersoft.branchengine.controller;

import com.tuckersoft.branchengine.dto.Dtos.*;
import com.tuckersoft.branchengine.service.DecisionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/decisions")
@RequiredArgsConstructor
public class DecisionController {
    private final DecisionService decisionService;

    @PostMapping
    public ResponseEntity<DecisionResponse> create(
            @Valid @RequestBody DecisionRequest req,
            @RequestHeader(value = "X-Bandersnatch-Simulate", required = false) String simulateHeader,
            Authentication auth
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(decisionService.createDecision(req, auth.getName(), simulateHeader));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<DecisionResponse>> list(
            @RequestParam(required = false) String branchType,
            @RequestParam(required = false) String impactLevel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long playthroughId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth
    ) {
        return ResponseEntity.ok(decisionService.getDecisions(branchType, impactLevel, status, playthroughId, page, size, auth.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DecisionResponse> getById(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(decisionService.getById(id, auth.getName()));
    }

    @GetMapping("/{id}/reality-logs")
    public ResponseEntity<List<RealityLogResponse>> getRealityLogs(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(decisionService.getRealityLogs(id, auth.getName()));
    }
}
