package com.marketintel.auth;

import com.marketintel.model.User;
import com.marketintel.persistence.DatabaseManager;
import com.marketintel.persistence.SQLiteUserRepository;
import com.marketintel.persistence.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    @TempDir
    Path tempDirectory;

    private AuthService authService;

    @BeforeEach
    void setUp() {

        Path databasePath =
                tempDirectory.resolve("test.db");

        DatabaseManager databaseManager =
                new DatabaseManager(
                        "jdbc:sqlite:"
                                + databasePath
                );

        databaseManager.initialize();

        UserRepository repository =
                new SQLiteUserRepository(
                        databaseManager
                );

        PasswordHasher passwordHasher =
                new BCryptPasswordHasher();

        authService =
                new AuthService(
                        repository,
                        passwordHasher
                );
    }

    @Test
    void registerCreatesUser() {

        User user = authService.register(
                "erdem",
                "SecurePassword123"
        );

        assertTrue(user.getId() > 0);
        assertEquals(
                "erdem",
                user.getUsername()
        );

        assertNotEquals(
                "SecurePassword123",
                user.getPasswordHash()
        );
    }

    @Test
    void registeredUserCanAuthenticate() {

        authService.register(
                "erdem",
                "SecurePassword123"
        );

        User user = authService.authenticate(
                "erdem",
                "SecurePassword123"
        );

        assertEquals(
                "erdem",
                user.getUsername()
        );
    }

    @Test
    void incorrectPasswordIsRejected() {

        authService.register(
                "erdem",
                "SecurePassword123"
        );

        assertThrows(
                SecurityException.class,
                () -> authService.authenticate(
                        "erdem",
                        "WrongPassword"
                )
        );
    }

    @Test
    void duplicateUsernameIsRejected() {

        authService.register(
                "erdem",
                "SecurePassword123"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(
                        "erdem",
                        "AnotherPassword123"
                )
        );
    }

    @Test
    void usernamesAreCaseInsensitive() {

        authService.register(
                "erdem",
                "SecurePassword123"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(
                        "ERDEM",
                        "AnotherPassword123"
                )
        );
    }
}