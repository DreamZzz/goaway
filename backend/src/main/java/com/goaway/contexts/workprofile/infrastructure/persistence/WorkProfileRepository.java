package com.goaway.contexts.workprofile.infrastructure.persistence;

import com.goaway.contexts.workprofile.domain.WorkProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkProfileRepository extends JpaRepository<WorkProfile, Long> {
    Optional<WorkProfile> findByUserId(Long userId);
}
