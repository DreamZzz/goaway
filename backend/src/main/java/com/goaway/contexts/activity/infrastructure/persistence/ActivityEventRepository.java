package com.goaway.contexts.activity.infrastructure.persistence;

import com.goaway.contexts.activity.domain.ActivityEvent;
import com.goaway.contexts.activity.domain.ActivityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ActivityEventRepository extends JpaRepository<ActivityEvent, Long> {

    long countByUserIdAndTypeAndOccurredAtBetween(Long userId, ActivityType type,
                                                  LocalDateTime start, LocalDateTime end);

    @Query("SELECT COALESCE(SUM(e.durationSeconds), 0) FROM ActivityEvent e " +
            "WHERE e.userId = :userId AND e.type = :type AND e.occurredAt BETWEEN :start AND :end")
    long sumDurationByUserIdAndTypeAndOccurredAtBetween(@Param("userId") Long userId,
                                                        @Param("type") ActivityType type,
                                                        @Param("start") LocalDateTime start,
                                                        @Param("end") LocalDateTime end);

    // ── 全时段聚合（徽章 / 单次榜基础数据）──

    long countByUserIdAndType(Long userId, ActivityType type);

    @Query("SELECT COALESCE(SUM(e.durationSeconds), 0) FROM ActivityEvent e " +
            "WHERE e.userId = :userId AND e.type = :type")
    long sumDurationByUserIdAndType(@Param("userId") Long userId, @Param("type") ActivityType type);

    @Query("SELECT COALESCE(MAX(e.durationSeconds), 0) FROM ActivityEvent e " +
            "WHERE e.userId = :userId AND e.type = :type")
    long maxDurationByUserIdAndType(@Param("userId") Long userId, @Param("type") ActivityType type);
}
