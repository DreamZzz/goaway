package com.goaway.platform.security;

public class GuestAccessErrorResponse {
    private final String error;
    private final String message;
    private final Integer guestTrialRemaining;

    public GuestAccessErrorResponse(String error, String message, Integer guestTrialRemaining) {
        this.error = error;
        this.message = message;
        this.guestTrialRemaining = guestTrialRemaining;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public Integer getGuestTrialRemaining() {
        return guestTrialRemaining;
    }
}
