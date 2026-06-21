package com.goaway.contexts.activity.infrastructure.persistence;

import com.goaway.contexts.activity.domain.BadgeDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BadgeDefinitionRepository extends JpaRepository<BadgeDefinition, Long> {

    List<BadgeDefinition> findByEnabledTrueOrderBySortOrderAscIdAsc();

    List<BadgeDefinition> findAllByOrderBySortOrderAscIdAsc();

    boolean existsByKey(String key);
}
