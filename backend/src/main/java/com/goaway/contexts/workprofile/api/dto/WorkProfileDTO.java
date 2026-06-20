package com.goaway.contexts.workprofile.api.dto;

import com.goaway.contexts.workprofile.domain.WorkProfile;

public class WorkProfileDTO {
    private Long userId;
    private String nickname;
    private String city;
    private String industry;
    private String jobType;
    private String gender;
    private String workStart;
    private String workEnd;
    private String hatedRelation;
    private String hatedNickname;
    private String hatedTraits;

    public WorkProfileDTO() {}

    public static WorkProfileDTO from(WorkProfile p) {
        WorkProfileDTO dto = new WorkProfileDTO();
        dto.userId = p.getUserId();
        dto.nickname = p.getNickname();
        dto.city = p.getCity();
        dto.industry = p.getIndustry();
        dto.jobType = p.getJobType();
        dto.gender = p.getGender();
        dto.workStart = p.getWorkStart();
        dto.workEnd = p.getWorkEnd();
        dto.hatedRelation = p.getHatedRelation();
        dto.hatedNickname = p.getHatedNickname();
        dto.hatedTraits = p.getHatedTraits();
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
}
