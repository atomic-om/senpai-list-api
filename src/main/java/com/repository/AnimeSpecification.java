package com.repository;

import com.entity.Anime;
import com.entity.Season;
import com.entity.WatchStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class AnimeSpecification {
    private AnimeSpecification() {
    }

    public static Specification<Anime> filterWatchlist(Long userId, String search, WatchStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("user").get("id"), userId));

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("title")), pattern));
            }

            if (status != null) {
                if (query != null) {
                    query.distinct(true);
                }
                Join<Anime, Season> seasonJoin = root.join("seasons", JoinType.INNER);
                predicates.add(cb.equal(seasonJoin.get("status"), status));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
