package com.goaway.contexts.roleplay.infrastructure.persistence;

import com.goaway.contexts.roleplay.domain.ContentReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentReportRepository extends JpaRepository<ContentReport, Long> {
}
