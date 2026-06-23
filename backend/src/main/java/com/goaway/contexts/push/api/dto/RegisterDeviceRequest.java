package com.goaway.contexts.push.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterDeviceRequest {

    @NotBlank(message = "deviceToken 不能为空")
    @Size(max = 200)
    private String deviceToken;

    @Size(max = 16)
    private String platform;

    public String getDeviceToken() { return deviceToken; }
    public void setDeviceToken(String deviceToken) { this.deviceToken = deviceToken; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
}
