package com.goaway.platform.security;

import org.springframework.http.HttpStatus;

public class GuestAccessException extends RuntimeException {
    private final HttpStatus status;
    private final String error;
    private final Integer guestTrialRemaining;

    public GuestAccessException(HttpStatus status, String error, String message, Integer guestTrialRemaining) {
        super(message);
        this.status = status;
        this.error = error;
        this.guestTrialRemaining = guestTrialRemaining;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public Integer getGuestTrialRemaining() {
        return guestTrialRemaining;
    }
}
