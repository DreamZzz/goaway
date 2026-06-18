package com.goaway.contexts.account.domain;

import jakarta.persistence.FetchType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "guest_profiles",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_guest_profiles_installation_hash", columnNames = "installation_hash")
        }
)
public class GuestProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "installation_hash", nullable = false, length = 64)
    private String installationHash;

    @Column(name = "trial_max_count", nullable = false)
    private Integer trialMaxCount = 3;

    @Column(name = "trial_used_count", nullable = false)
    private Integer trialUsedCount = 0;

    @Column(name = "last_auth_at")
    private LocalDateTime lastAuthAt;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Column(name = "first_seen_ip_hash", length = 64)
    private String firstSeenIpHash;

    @Column(name = "last_seen_ip_hash", length = 64)
    private String lastSeenIpHash;

    @Column(name = "blocked_until")
    private LocalDateTime blockedUntil;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getInstallationHash() {
        return installationHash;
    }

    public void setInstallationHash(String installationHash) {
        this.installationHash = installationHash;
    }

    public String getInstallIdHash() {
        return installationHash;
    }

    public void setInstallIdHash(String installIdHash) {
        this.installationHash = installIdHash;
    }

    public Integer getTrialMaxCount() {
        return trialMaxCount;
    }

    public void setTrialMaxCount(Integer trialMaxCount) {
        this.trialMaxCount = trialMaxCount;
    }

    public Integer getTrialUsedCount() {
        return trialUsedCount;
    }

    public void setTrialUsedCount(Integer trialUsedCount) {
        this.trialUsedCount = trialUsedCount;
    }

    public LocalDateTime getLastAuthAt() {
        return lastAuthAt;
    }

    public void setLastAuthAt(LocalDateTime lastAuthAt) {
        this.lastAuthAt = lastAuthAt;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(LocalDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public String getFirstSeenIpHash() {
        return firstSeenIpHash;
    }

    public void setFirstSeenIpHash(String firstSeenIpHash) {
        this.firstSeenIpHash = firstSeenIpHash;
    }

    public String getLastSeenIpHash() {
        return lastSeenIpHash;
    }

    public void setLastSeenIpHash(String lastSeenIpHash) {
        this.lastSeenIpHash = lastSeenIpHash;
    }

    public LocalDateTime getBlockedUntil() {
        return blockedUntil;
    }

    public void setBlockedUntil(LocalDateTime blockedUntil) {
        this.blockedUntil = blockedUntil;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getTrialRemaining() {
        int max = trialMaxCount == null ? 0 : trialMaxCount;
        int used = trialUsedCount == null ? 0 : trialUsedCount;
        return Math.max(0, max - used);
    }
}
