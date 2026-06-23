package com.goaway.contexts.push.infrastructure.persistence;

import com.goaway.contexts.push.domain.PushDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PushDeviceRepository extends JpaRepository<PushDevice, Long> {
    Optional<PushDevice> findByDeviceToken(String deviceToken);

    List<PushDevice> findByUserId(Long userId);

    void deleteByDeviceToken(String deviceToken);
}
