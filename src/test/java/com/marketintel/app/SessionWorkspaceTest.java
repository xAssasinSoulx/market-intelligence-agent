package com.marketintel.app;

import com.marketintel.auth.AuthService;
import com.marketintel.auth.BCryptPasswordHasher;
import com.marketintel.auth.SessionManager;
import com.marketintel.model.AssetType;
import com.marketintel.model.Portfolio;
import com.marketintel.persistence.DatabaseManager;
import com.marketintel.persistence.SQLitePortfolioRepository;
import com.marketintel.persistence.SQLiteUserRepository;
import com.marketintel.persistence.SQLiteWatchlistRepository;
import com.marketintel.services.PortfolioService;
import com.marketintel.services.WatchlistService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionWorkspaceTest {

    @TempDir
    Path tempDirectory;

    private SessionWorkspace workspace;

    @BeforeEach
    void setUp() {
        Path databasePath = tempDirectory.resolve("workspace.db");
        DatabaseManager databaseManager =
                new DatabaseManager("jdbc:sqlite:" + databasePath);
        databaseManager.initialize();

        AuthService authService = new AuthService(
                new SQLiteUserRepository(databaseManager),
                new BCryptPasswordHasher()
        );

        workspace = new SessionWorkspace(
                authService,
                new SessionManager(),
                new PortfolioService(new SQLitePortfolioRepository(databaseManager)),
                new WatchlistService(new SQLiteWatchlistRepository(databaseManager))
        );
    }

    @Test
    void registerDoesNotStartASession() {
        workspace.register("ada", "SecurePassword123");

        assertFalse(workspace.isAuthenticated());
        assertThrows(IllegalStateException.class, workspace::portfolio);
    }

    @Test
    void loginIsRequiredBeforeReadingAPortfolio() {
        workspace.register("ada", "SecurePassword123");

        assertThrows(SecurityException.class,
                () -> workspace.login("ada", "wrong-password"));
        assertFalse(workspace.isAuthenticated());
    }

    @Test
    void portfolioAndWatchlistStayWithTheSignedInUser() {
        workspace.register("ada", "SecurePassword123");
        workspace.register("grace", "SecurePassword123");

        workspace.login("ada", "SecurePassword123");
        workspace.addPosition("NVDA", new BigDecimal("2"), AssetType.STOCK);
        workspace.addToWatchlist("qqq");

        Portfolio adaPortfolio = workspace.portfolio();
        assertEquals(1, adaPortfolio.getPositions().size());
        assertEquals("NVDA", adaPortfolio.getPositions().getFirst().getSymbol());
        assertTrue(workspace.watchlist().contains("QQQ"));

        workspace.logout();
        workspace.login("grace", "SecurePassword123");

        assertTrue(workspace.portfolio().getPositions().isEmpty());
        assertTrue(workspace.watchlist().getSymbols().isEmpty());
        assertFalse(workspace.removePosition("NVDA"));

        workspace.logout();
        assertThrows(IllegalStateException.class, workspace::watchlist);
    }
}
