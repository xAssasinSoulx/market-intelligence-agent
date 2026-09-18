package com.marketintel.auth;

import com.marketintel.model.User;
import com.marketintel.model.UserSession;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SessionManagerTest {

    @Test
    void sessionCanBeStarted() {

        User user = new User(
                1,
                "erdem",
                "hash",
                LocalDateTime.now()
        );

        SessionManager manager =
                new SessionManager();

        UserSession session =
                manager.startSession(user);

        assertTrue(manager.isAuthenticated());
        assertEquals(
                1,
                session.getUserId()
        );

        assertEquals(
                user,
                manager.getCurrentUser()
        );
    }

    @Test
    void logoutClearsSession() {

        User user = new User(
                1,
                "erdem",
                "hash",
                LocalDateTime.now()
        );

        SessionManager manager =
                new SessionManager();

        manager.startSession(user);
        manager.endSession();

        assertFalse(
                manager.isAuthenticated()
        );

        assertThrows(
                IllegalStateException.class,
                manager::getCurrentUser
        );
    }
}