package com.tuckersoft.branchengine.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "story_nodes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StoryNode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String nodeCode;

    @Column(nullable = false, length = 80)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String sceneText;

    @Column(nullable = false)
    private Integer branchCapacity;

    @Column(nullable = false)
    private Integer currentBranches;

    @Column
    private String primaryBranchCode;

    @Column
    private String glitchBranchCode;

    @Column(nullable = false)
    private Instant createdAt;
}
