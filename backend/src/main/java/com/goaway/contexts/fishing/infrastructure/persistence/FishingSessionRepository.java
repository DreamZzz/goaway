package com.goaway.contexts.fishing.infrastructure.persistence;

import com.goaway.contexts.fishing.domain.FishingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface FishingSessionRepository extends JpaRepository<FishingSession, Long> {

    Optional<FishingSession> findByUserIdAndSessionDate(Long userId, LocalDate sessionDate);

    @Query("SELECT COALESCE(SUM(f.totalSeconds), 0) FROM FishingSession f WHERE f.userId = :userId")
    long sumTotalSecondsByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(f.totalSeconds), 0) FROM FishingSession f " +
            "WHERE f.userId = :userId AND f.sessionDate BETWEEN :start AND :end")
    long sumSecondsByUserIdAndDateBetween(@Param("userId") Long userId,
                                          @Param("start") LocalDate start,
                                          @Param("end") LocalDate end);
}
