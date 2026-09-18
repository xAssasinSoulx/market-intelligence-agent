package com.marketintel;

import com.marketintel.auth.AuthService;
import com.marketintel.auth.BCryptPasswordHasher;
import com.marketintel.auth.PasswordHasher;
import com.marketintel.auth.SessionManager;

import com.marketintel.persistence.DatabaseManager;
import com.marketintel.persistence.SQLiteUserRepository;
import com.marketintel.persistence.UserRepository;

import com.marketintel.ui.CommandParser;
import com.marketintel.ui.TerminalUI;
import com.marketintel.ui.UserInterface;

import com.marketintel.persistence.PortfolioRepository;
import com.marketintel.persistence.SQLitePortfolioRepository;
import com.marketintel.persistence.WatchlistRepository;
import com.marketintel.persistence.SQLiteWatchlistRepository;

import com.marketintel.services.PortfolioService;
import com.marketintel.services.WatchlistService;

import com.marketintel.providers.MarketDataApiClient;
import com.marketintel.providers.MarketDataProvider;

import com.marketintel.agent.ToolManager;

import com.marketintel.providers.MarketDataApiClient;
import com.marketintel.providers.MarketDataProvider;

import com.marketintel.services.RequestValidator;

import com.marketintel.tools.MarketDataTool;

public class Main {

    private static final String DATABASE_URL =
            "jdbc:sqlite:data/market-intelligence.db";

    public static void main(String[] args) {

        // ----------------------------------------------------
        // Persistence
        // ----------------------------------------------------

        ToolManager toolManager =
                new ToolManager();

        RequestValidator requestValidator =
                new RequestValidator();

        DatabaseManager databaseManager =
                new DatabaseManager(DATABASE_URL);

        databaseManager.initialize();

        UserRepository userRepository =
                new SQLiteUserRepository(
                        databaseManager
                );
        PortfolioRepository portfolioRepository =
                new SQLitePortfolioRepository(
                        databaseManager
                );

        WatchlistRepository watchlistRepository =
                new SQLiteWatchlistRepository(
                        databaseManager
                );

        PortfolioService portfolioService =
                new PortfolioService(
                        portfolioRepository
                );

        WatchlistService watchlistService =
                new WatchlistService(
                        watchlistRepository
                );

        // ----------------------------------------------------
        // Authentication
        // ----------------------------------------------------

        PasswordHasher passwordHasher =
                new BCryptPasswordHasher();

        AuthService authService =
                new AuthService(
                        userRepository,
                        passwordHasher
                );

        SessionManager sessionManager =
                new SessionManager();

        // ----------------------------------------------------
        // User Interface
        // ----------------------------------------------------

        CommandParser commandParser =
                new CommandParser();

        String marketApiKey =
                System.getenv(
                        "ALPHA_VANTAGE_API_KEY"
                );

        if (marketApiKey != null
                && !marketApiKey.isBlank()) {

            MarketDataProvider marketDataProvider =
                    new MarketDataApiClient(
                            marketApiKey
                    );

            MarketDataTool marketDataTool =
                    new MarketDataTool(
                            marketDataProvider,
                            requestValidator
                    );

            toolManager.registerTool(
                    marketDataTool
            );

        } else {

            System.err.println(
                    "Warning: Market-data API key "
                            + "is not configured."
            );
        }

        UserInterface userInterface =
                new TerminalUI(
                        authService,
                        sessionManager,
                        commandParser,
                        portfolioService,
                        watchlistService,
                        toolManager
                );

        userInterface.start();
    }
}