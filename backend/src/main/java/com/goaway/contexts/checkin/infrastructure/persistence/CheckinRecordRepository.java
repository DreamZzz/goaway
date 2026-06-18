package com.goaway.contexts.checkin.infrastructure.persistence;

import com.goaway.contexts.checkin.domain.CheckinRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface CheckinRecordRepository extends JpaRepository<CheckinRecord, Long> {

    Optional<CheckinRecord> findByUserIdAndCheckinDate(Long userId, LocalDate checkinDate);

    Optional<CheckinRecord> findTopByUserIdOrderByCheckinDateDesc(Long userId);

    long countByUserId(Long userId);

    long countByUserIdAndCheckinDateBetween(Long userId, LocalDate start, LocalDate end);
}
