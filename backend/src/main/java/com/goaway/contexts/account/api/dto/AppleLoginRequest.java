package com.goaway.contexts.account.api.dto;

import jakarta.validation.constraints.NotBlank;

public class AppleLoginRequest {

    @NotBlank
    private String identityToken;

    /** User's full name — only present on first authorization, null on subsequent logins. */
    private String fullName;

    /** User's email — only present on first authorization (real or Apple relay address). */
    private String email;

    public String getIdentityToken() { return identityToken; }
    public void setIdentityToken(String identityToken) { this.identityToken = identityToken; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
