package com.goaway.contexts.push.infrastructure.persistence;

import com.goaway.contexts.push.domain.PushPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PushPreferenceRepository extends JpaRepository<PushPreference, Long> {
    Optional<PushPreference> findByUserId(Long userId);

    /** 已开启且非 OFF 的偏好，供定时巡检批量取用。 */
    @Query("select p from PushPreference p where p.enabled = true and p.frequency <> com.goaway.contexts.push.domain.PushFrequency.OFF")
    List<PushPreference> findActivePushTargets();

    /** 不活跃召回候选：开启推送且 lastActiveAt 早于阈值（或从未活跃）。 */
    @Query("select p from PushPreference p where p.enabled = true and p.frequency <> com.goaway.contexts.push.domain.PushFrequency.OFF "
            + "and (p.lastActiveAt is null or p.lastActiveAt < :threshold)")
    List<PushPreference> findRecallTargets(@Param("threshold") LocalDateTime threshold);
}
