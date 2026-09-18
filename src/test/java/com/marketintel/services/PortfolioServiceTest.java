package com.marketintel.services;

import com.marketintel.auth.AuthService;
import com.marketintel.auth.BCryptPasswordHasher;
import com.marketintel.model.AssetType;
import com.marketintel.model.Portfolio;
import com.marketintel.model.User;
import com.marketintel.persistence.DatabaseManager;
import com.marketintel.persistence.SQLitePortfolioRepository;
import com.marketintel.persistence.SQLiteUserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PortfolioServiceTest {

    @TempDir
    Path tempDirectory;

    private PortfolioService portfolioService;
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

        SQLiteUserRepository userRepository =
                new SQLiteUserRepository(
                        databaseManager
                );

        AuthService authService =
                new AuthService(
                        userRepository,
                        new BCryptPasswordHasher()
                );

        User user =
                authService.register(
                        "portfolioUser",
                        "SecurePassword123"
                );

        userId = user.getId();

        portfolioService =
                new PortfolioService(
                        new SQLitePortfolioRepository(
                                databaseManager
                        )
                );
    }

    @Test
    void newPortfolioIsEmpty() {

        Portfolio portfolio =
                portfolioService.getPortfolio(
                        userId
                );

        assertTrue(
                portfolio.getPositions().isEmpty()
        );
    }

    @Test
    void positionCanBeAdded() {

        portfolioService.addPosition(
                userId,
                "XEQT",
                new BigDecimal("10"),
                AssetType.ETF
        );

        Portfolio portfolio =
                portfolioService.getPortfolio(
                        userId
                );

        assertEquals(
                1,
                portfolio.getPositions().size()
        );

        assertEquals(
                "XEQT",
                portfolio.getPositions()
                        .getFirst()
                        .getSymbol()
        );
    }

    @Test
    void portfolioPersistsPosition() {

        portfolioService.addPosition(
                userId,
                "NVDA",
                new BigDecimal("2.5"),
                AssetType.STOCK
        );

        Portfolio reloaded =
                portfolioService.getPortfolio(
                        userId
                );

        assertEquals(
                new BigDecimal("2.5"),
                reloaded.getPositions()
                        .getFirst()
                        .getQuantity()
        );
    }

    @Test
    void duplicatePositionIsRejected() {

        portfolioService.addPosition(
                userId,
                "XEQT",
                new BigDecimal("10"),
                AssetType.ETF
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> portfolioService.addPosition(
                        userId,
                        "xeqt",
                        new BigDecimal("5"),
                        AssetType.ETF
                )
        );
    }

    @Test
    void positionCanBeRemoved() {

        portfolioService.addPosition(
                userId,
                "MSFT",
                new BigDecimal("4"),
                AssetType.STOCK
        );

        assertTrue(
                portfolioService.removePosition(
                        userId,
                        "MSFT"
                )
        );

        assertTrue(
                portfolioService
                        .getPortfolio(userId)
                        .getPositions()
                        .isEmpty()
        );
    }

    @Test
    void invalidQuantityIsRejected() {

        assertThrows(
                IllegalArgumentException.class,
                () -> portfolioService.addPosition(
                        userId,
                        "NVDA",
                        BigDecimal.ZERO,
                        AssetType.STOCK
                )
        );
    }

    @Test
    void portfoliosAreIsolatedBetweenUsers() {

        DatabaseManager databaseManager =
                new DatabaseManager(
                        "jdbc:sqlite:"
                                + tempDirectory.resolve(
                                "isolation.db"
                        )
                );

        databaseManager.initialize();

        AuthService authService =
                new AuthService(
                        new SQLiteUserRepository(
                                databaseManager
                        ),
                        new BCryptPasswordHasher()
                );

        User first =
                authService.register(
                        "userOne",
                        "SecurePassword123"
                );

        User second =
                authService.register(
                        "userTwo",
                        "SecurePassword123"
                );

        PortfolioService service =
                new PortfolioService(
                        new SQLitePortfolioRepository(
                                databaseManager
                        )
                );

        service.addPosition(
                first.getId(),
                "NVDA",
                BigDecimal.ONE,
                AssetType.STOCK
        );

        assertEquals(
                1,
                service.getPortfolio(
                        first.getId()
                ).getPositions().size()
        );

        assertTrue(
                service.getPortfolio(
                        second.getId()
                ).getPositions().isEmpty()
        );
    }
}