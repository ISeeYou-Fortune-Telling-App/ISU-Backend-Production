package com.iseeyou.fortunetelling.exception;

import lombok.Getter;

@Getter
public class AccountBannedException extends RuntimeException {
    private final String reason;

    public AccountBannedException(String message, String reason) {
        super(message);
        this.reason = reason;
    }

    public AccountBannedException(String message, String reason, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }
}

