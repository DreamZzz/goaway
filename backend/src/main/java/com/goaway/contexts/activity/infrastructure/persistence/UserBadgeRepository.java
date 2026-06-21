package com.goaway.contexts.activity.infrastructure.persistence;

import com.goaway.contexts.activity.domain.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {

    List<UserBadge> findByUserId(Long userId);

    List<UserBadge> findByUserIdAndEarnedAtBetween(Long userId,
                                                   java.time.LocalDateTime start,
                                                   java.time.LocalDateTime end);

    boolean existsByUserIdAndBadgeKey(Long userId, String badgeKey);
}
