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

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }

    public String getJobType() { return jobType; }
    public void setJobType(String jobType) { this.jobType = jobType; }
}
