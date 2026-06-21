package com.goaway.contexts.activity.infrastructure.persistence;

import com.goaway.contexts.activity.domain.LeaderboardDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeaderboardDefinitionRepository extends JpaRepository<LeaderboardDefinition, Long> {

    List<LeaderboardDefinition> findByEnabledTrueOrderBySortOrderAscIdAsc();

    List<LeaderboardDefinition> findAllByOrderBySortOrderAscIdAsc();

    Optional<LeaderboardDefinition> findByKeyAndEnabledTrue(String key);

    boolean existsByKey(String key);
}
