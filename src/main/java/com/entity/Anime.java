package com.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "animes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "title"})
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Anime {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String coverImageUrl;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "anime", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("seasonNumber ASC")
    @BatchSize(size = 20)
    @Builder.Default
    private List<Season> seasons = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void addSeason(Season season) {
        seasons.add(season);
        season.setAnime(this);
    }

    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
