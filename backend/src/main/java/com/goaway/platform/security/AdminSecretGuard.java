package com.goaway.platform.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * /admin/** API 的密钥校验：请求头 X-Admin-Secret 必须等于 app.admin.secret。
 * 未配置密钥（为空）时一律拒绝，避免误开放。
 */
@Component
public class AdminSecretGuard {

    @Value("${app.admin.secret:}")
    private String adminSecret;

    public void verify(String provided) {
        if (adminSecret == null || adminSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "运营后台未配置密钥");
        }
        if (provided == null || !adminSecret.equals(provided)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "密钥无效");
        }
    }
}
