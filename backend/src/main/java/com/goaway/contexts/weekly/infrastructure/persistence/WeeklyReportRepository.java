package com.goaway.contexts.weekly.infrastructure.persistence;

import com.goaway.contexts.weekly.domain.WeeklyReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WeeklyReportRepository extends JpaRepository<WeeklyReport, Long> {

    List<WeeklyReport> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<WeeklyReport> findByIdAndUserId(Long id, Long userId);
}
