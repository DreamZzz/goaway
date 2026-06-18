package com.goaway.contexts.account.infrastructure.persistence;

import com.goaway.contexts.account.domain.GuestProfile;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GuestProfileRepository extends JpaRepository<GuestProfile, Long> {

    Optional<GuestProfile> findByInstallationHash(String installationHash);

    Optional<GuestProfile> findByUser_Id(Long userId);

    default Optional<GuestProfile> findByInstallIdHash(String installIdHash) {
        return findByInstallationHash(installIdHash);
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM GuestProfile g WHERE g.installationHash = :installationHash")
    Optional<GuestProfile> findByInstallationHashForUpdate(@Param("installationHash") String installationHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM GuestProfile g WHERE g.id = :id")
    Optional<GuestProfile> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM GuestProfile g WHERE g.user.id = :userId")
    Optional<GuestProfile> findByUserIdForUpdate(@Param("userId") Long userId);
}
