package com.goaway.contexts.workprofile.api.dto;

import jakarta.validation.constraints.Size;

public class UpsertWorkProfileRequest {

    @Size(max = 30, message = "昵称最多 30 个字符")
    private String nickname;

    @Size(max = 40)
    private String city;

    @Size(max = 40)
    private String industry;

    @Size(max = 40)
    private String jobType;

    @Size(max = 16)
    private String gender;

    @Size(max = 8)
    private String workStart;

    @Size(max = 8)
    private String workEnd;

    @Size(max = 40)
    private String hatedRelation;

    @Size(max = 40)
    private String hatedNickname;

    @Size(max = 300)
    private String hatedTraits;

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
