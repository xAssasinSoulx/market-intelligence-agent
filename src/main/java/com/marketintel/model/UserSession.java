package com.marketintel.model;

import java.time.LocalDateTime;

public class UserSession {

    private final String sessionId;
    private final long userId;
    private final LocalDateTime loginTime;

    public UserSession(String sessionId,
                       long userId,
                       LocalDateTime loginTime) {

        this.sessionId = sessionId;
        this.userId = userId;
        this.loginTime = loginTime;
    }

    public String getSessionId() {
        return sessionId;
    }

    public long getUserId() {
        return userId;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }
}