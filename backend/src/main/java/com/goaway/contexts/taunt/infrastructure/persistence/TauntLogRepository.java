package com.goaway.contexts.taunt.infrastructure.persistence;

import com.goaway.contexts.taunt.domain.TauntLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface TauntLogRepository extends JpaRepository<TauntLog, Long> {

    /** 某用户在某时刻之后成功发送的条数，用于每日上限频控。 */
    long countByUserIdAndSuccessTrueAndSentAtAfter(Long userId, LocalDateTime after);
}
