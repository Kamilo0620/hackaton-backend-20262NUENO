package com.tuckersoft.branchengine.controller;

import com.tuckersoft.branchengine.dto.Dtos.*;
import com.tuckersoft.branchengine.service.StoryNodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/nodes")
@RequiredArgsConstructor
public class StoryNodeController {
    private final StoryNodeService nodeService;

    @PostMapping
    public ResponseEntity<StoryNodeResponse> create(@Valid @RequestBody StoryNodeRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(nodeService.createNode(req));
    }

    @GetMapping
    public ResponseEntity<List<StoryNodeResponse>> getAll() {
        return ResponseEntity.ok(nodeService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StoryNodeResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(nodeService.getById(id));
    }
}
