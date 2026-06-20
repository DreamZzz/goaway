package com.goaway.contexts.workprofile.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 打工人画像：匿名昵称 + 城市 / 行业 / 工种。
 * 既用于排行榜的切片维度，也用于榜单展示（不暴露真实身份）。
 * 每个用户至多一条，userId 唯一。
 */
@Entity
@Table(name = "work_profiles", indexes = {
        @Index(name = "idx_work_profiles_city", columnList = "city"),
        @Index(name = "idx_work_profiles_industry", columnList = "industry"),
        @Index(name = "idx_work_profiles_job_type", columnList = "job_type")
})
public class WorkProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(length = 30)
    private String nickname;

    @Column(length = 40)
    private String city;

    @Column(length = 40)
    private String industry;

    @Column(name = "job_type", length = 40)
    private String jobType;

    @Column(length = 16)
    private String gender;

    @Column(name = "work_start", length = 8)
    private String workStart;

    @Column(name = "work_end", length = 8)
    private String workEnd;

    @Column(name = "hated_relation", length = 40)
    private String hatedRelation;

    @Column(name = "hated_nickname", length = 40)
    private String hatedNickname;

    @Column(name = "hated_traits", length = 300)
    private String hatedTraits;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public WorkProfile() {}

    public WorkProfile(Long userId) {
        this.userId = userId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }

    public String getJobType() { return jobType; }
    public void setJobType(String jobType) { this.jobType = jobType; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getWorkStart() { return workStart; }
    public void setWorkStart(String workStart) { this.workStart = workStart; }

    public String getWorkEnd() { return workEnd; }
    public void setWorkEnd(String workEnd) { this.workEnd = workEnd; }

    public String getHatedRelation() { return hatedRelation; }
    public void setHatedRelation(String hatedRelation) { this.hatedRelation = hatedRelation; }

    public String getHatedNickname() { return hatedNickname; }
    public void setHatedNickname(String hatedNickname) { this.hatedNickname = hatedNickname; }

    public String getHatedTraits() { return hatedTraits; }
    public void setHatedTraits(String hatedTraits) { this.hatedTraits = hatedTraits; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
