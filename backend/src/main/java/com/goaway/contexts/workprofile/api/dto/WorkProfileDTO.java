package com.goaway.contexts.workprofile.api.dto;

import com.goaway.contexts.workprofile.domain.WorkProfile;

public class WorkProfileDTO {
    private Long userId;
    private String nickname;
    private String city;
    private String industry;
    private String jobType;

    public WorkProfileDTO() {}

    public static WorkProfileDTO from(WorkProfile profile) {
        WorkProfileDTO dto = new WorkProfileDTO();
        dto.userId = profile.getUserId();
        dto.nickname = profile.getNickname();
        dto.city = profile.getCity();
        dto.industry = profile.getIndustry();
        dto.jobType = profile.getJobType();
        return dto;
    }

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
}
