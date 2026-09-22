package com.tuckersoft.branchengine.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "playthroughs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Playthrough {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String playerTag;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String startNodeCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_node_id", nullable = false)
    private StoryNode currentNode;

    @Column(nullable = false)
    private Integer lucidity;

    @Column(nullable = false)
    private Integer controlLevel;

    @Column(nullable = false)
    private String status;

    @Column
    private String endingCode;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;
}
