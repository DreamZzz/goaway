package com.goaway.contexts.account.api.dto;

public class AuthResponse {
    private String token;
    private String type = "Bearer";
    private Long id;
    private String username;
    private String displayName;
    private String email;
    private String phone;
    private String avatarUrl;
    private Boolean isGuest = false;
    private Integer guestTrialRemaining;
    
    public AuthResponse(String token, Long id, String username, String displayName, String email, String phone, String avatarUrl) {
        this(token, id, username, displayName, email, phone, avatarUrl, false, null);
    }

    public AuthResponse(
            String token,
            Long id,
            String username,
            String displayName,
            String email,
            String phone,
            String avatarUrl,
            Boolean isGuest,
            Integer guestTrialRemaining
    ) {
        this.token = token;
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.email = email;
        this.phone = phone;
        this.avatarUrl = avatarUrl;
        this.isGuest = isGuest;
        this.guestTrialRemaining = guestTrialRemaining;
    }
    
    // Getters and Setters
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getAvatarUrl() {
        return avatarUrl;
    }
    
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Boolean getIsGuest() {
        return isGuest;
    }

    public void setIsGuest(Boolean guest) {
        isGuest = guest;
    }

    public Integer getGuestTrialRemaining() {
        return guestTrialRemaining;
    }

    public void setGuestTrialRemaining(Integer guestTrialRemaining) {
        this.guestTrialRemaining = guestTrialRemaining;
    }
}
