package com.tuckersoft.branchengine.controller;

import com.tuckersoft.branchengine.dto.Dtos.*;
import com.tuckersoft.branchengine.service.PlaythroughService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/playthroughs")
@RequiredArgsConstructor
public class PlaythroughController {
    private final PlaythroughService playthroughService;

    @PostMapping
    public ResponseEntity<PlaythroughResponse> create(@Valid @RequestBody PlaythroughRequest req, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(playthroughService.create(req, auth.getName()));
    }

    @GetMapping
    public ResponseEntity<List<PlaythroughResponse>> getAll(Authentication auth) {
        return ResponseEntity.ok(playthroughService.getAll(auth.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlaythroughResponse> getById(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(playthroughService.getById(id, auth.getName()));
    }

    @GetMapping("/{id}/path")
    public ResponseEntity<PlaythroughPathResponse> getPath(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(playthroughService.getPath(id, auth.getName()));
    }
}
