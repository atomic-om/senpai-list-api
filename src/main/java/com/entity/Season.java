package com.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "seasons", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"anime_id", "season_number"})
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Season {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "season_number", nullable = false)
    private Integer seasonNumber;

    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private WatchStatus status = WatchStatus.PLAN_TO_WATCH;

    @Column(nullable = false)
    @Builder.Default
    private Integer episodesWatched = 0;

    private Integer totalEpisodes;

    private Integer rating;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "anime_id", nullable = false)
    private Anime anime;
}
