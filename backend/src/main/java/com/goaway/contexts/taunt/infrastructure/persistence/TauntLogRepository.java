package com.goaway.contexts.taunt.infrastructure.persistence;

import com.goaway.contexts.taunt.domain.TauntLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TauntLogRepository extends JpaRepository<TauntLog, Long> {

    /** 某用户在某时刻之后成功发送的条数，用于每日上限频控。 */
    long countByUserIdAndSuccessTrueAndSentAtAfter(Long userId, LocalDateTime after);

    /** 收件箱同步：某用户成功发出的、id 大于游标的毒舌，按 id 升序取前 50 条。 */
    List<TauntLog> findTop50ByUserIdAndSuccessTrueAndIdGreaterThanOrderByIdAsc(Long userId, Long sinceId);
}
