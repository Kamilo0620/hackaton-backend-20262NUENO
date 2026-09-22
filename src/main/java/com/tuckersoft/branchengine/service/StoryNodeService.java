package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.dto.Dtos.*;
import com.tuckersoft.branchengine.exception.BusinessException;
import com.tuckersoft.branchengine.exception.ConflictException;
import com.tuckersoft.branchengine.exception.ResourceNotFoundException;
import com.tuckersoft.branchengine.model.StoryNode;
import com.tuckersoft.branchengine.repository.StoryNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoryNodeService {
    private final StoryNodeRepository nodeRepository;

    public StoryNodeResponse createNode(StoryNodeRequest req) {
        if (nodeRepository.existsByNodeCode(req.nodeCode())) {
            throw new ConflictException("El nodeCode ya existe");
        }
        if (req.branchCapacity() <= 0) {
            throw new BusinessException("branchCapacity debe ser mayor a 0");
        }

        StoryNode node = StoryNode.builder()
                .nodeCode(req.nodeCode())
                .title(req.title())
                .sceneText(req.sceneText())
                .branchCapacity(req.branchCapacity())
                .currentBranches(0)
                .primaryBranchCode(req.primaryBranchCode())
                .glitchBranchCode(req.glitchBranchCode())
                .createdAt(Instant.now())
                .build();
        nodeRepository.save(node);

        return toDto(node);
    }

    public List<StoryNodeResponse> getAll() {
        return nodeRepository.findAll().stream().map(this::toDto).toList();
    }

    public StoryNodeResponse getById(Long id) {
        StoryNode node = nodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StoryNode no encontrado"));
        return toDto(node);
    }

    private StoryNodeResponse toDto(StoryNode n) {
        return new StoryNodeResponse(
                n.getId(), n.getNodeCode(), n.getTitle(), n.getSceneText(),
                n.getBranchCapacity(), n.getCurrentBranches(),
                n.getPrimaryBranchCode(), n.getGlitchBranchCode(), n.getCreatedAt()
        );
    }
}
