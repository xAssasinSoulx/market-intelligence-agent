package com.marketintel.services;

import com.marketintel.auth.AuthService;
import com.marketintel.auth.BCryptPasswordHasher;
import com.marketintel.model.User;
import com.marketintel.persistence.DatabaseManager;
import com.marketintel.persistence.SQLiteUserRepository;
import com.marketintel.persistence.SQLiteWatchlistRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class WatchlistServiceTest {

    @TempDir
    Path tempDirectory;

    private WatchlistService watchlistService;
    private long userId;

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

        AuthService authService =
                new AuthService(
                        new SQLiteUserRepository(
                                databaseManager
                        ),
                        new BCryptPasswordHasher()
                );

        User user =
                authService.register(
                        "watchlistUser",
                        "SecurePassword123"
                );

        userId = user.getId();

        watchlistService =
                new WatchlistService(
                        new SQLiteWatchlistRepository(
                                databaseManager
                        )
                );
    }

    @Test
    void newWatchlistIsEmpty() {

        assertTrue(
                watchlistService
                        .getWatchlist(userId)
                        .getSymbols()
                        .isEmpty()
        );
    }

    @Test
    void symbolCanBeAdded() {

        watchlistService.addSymbol(
                userId,
                "nvda"
        );

        assertTrue(
                watchlistService
                        .getWatchlist(userId)
                        .contains("NVDA")
        );
    }

    @Test
    void duplicateSymbolIsRejected() {

        watchlistService.addSymbol(
                userId,
                "NVDA"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> watchlistService.addSymbol(
                        userId,
                        "nvda"
                )
        );
    }

    @Test
    void symbolCanBeRemoved() {

        watchlistService.addSymbol(
                userId,
                "MSFT"
        );

        assertTrue(
                watchlistService.removeSymbol(
                        userId,
                        "MSFT"
                )
        );

        assertFalse(
                watchlistService
                        .getWatchlist(userId)
                        .contains("MSFT")
        );
    }
}