package com.marketintel.auth;

import com.marketintel.model.User;
import com.marketintel.model.UserSession;

import java.time.LocalDateTime;
import java.util.UUID;

public class SessionManager {

    private UserSession currentSession;
    private User currentUser;

    public UserSession startSession(User user) {

        if (user == null) {
            throw new IllegalArgumentException(
                    "User cannot be null."
            );
        }

        currentUser = user;

        currentSession = new UserSession(
                UUID.randomUUID().toString(),
                user.getId(),
                LocalDateTime.now()
        );

        return currentSession;
    }

    public void endSession() {
        currentSession = null;
        currentUser = null;
    }

    public boolean isAuthenticated() {
        return currentSession != null
                && currentUser != null;
    }

    public User getCurrentUser() {

        requireAuthenticated();

        return currentUser;
    }

    public long getCurrentUserId() {

        requireAuthenticated();

        return currentSession.getUserId();
    }

    public UserSession getCurrentSession() {

        requireAuthenticated();

        return currentSession;
    }

    private void requireAuthenticated() {

        if (!isAuthenticated()) {
            throw new IllegalStateException(
                    "No authenticated user session exists."
            );
        }
    }
}