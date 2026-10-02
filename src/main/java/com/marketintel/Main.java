package com.marketintel;

import com.marketintel.agent.ToolManager;

import com.marketintel.auth.AuthService;
import com.marketintel.auth.BCryptPasswordHasher;
import com.marketintel.auth.PasswordHasher;
import com.marketintel.auth.SessionManager;

import com.marketintel.persistence.DatabaseManager;
import com.marketintel.persistence.PortfolioRepository;
import com.marketintel.persistence.SQLitePortfolioRepository;
import com.marketintel.persistence.SQLiteUserRepository;
import com.marketintel.persistence.SQLiteWatchlistRepository;
import com.marketintel.persistence.UserRepository;
import com.marketintel.persistence.WatchlistRepository;

import com.marketintel.providers.AlphaVantageClient;
import com.marketintel.providers.ETFDataApiClient;
import com.marketintel.providers.ETFDataProvider;
import com.marketintel.providers.MarketDataApiClient;
import com.marketintel.providers.MarketDataProvider;

import com.marketintel.services.ExposureCalculator;
import com.marketintel.services.PortfolioService;
import com.marketintel.services.RequestValidator;
import com.marketintel.services.WatchlistService;

import com.marketintel.tools.ComparisonTool;
import com.marketintel.tools.ETFExposureTool;
import com.marketintel.tools.MarketDataTool;
import com.marketintel.providers.NewsApiClient;
import com.marketintel.providers.NewsProvider;
import com.marketintel.tools.NewsTool;

import com.marketintel.ui.CommandParser;
import com.marketintel.ui.TerminalUI;
import com.marketintel.ui.UserInterface;
import com.marketintel.services.SecurityComparisonCalculator;


public class Main {

    private static final String DATABASE_URL =
            "jdbc:sqlite:data/market-intelligence.db";

    public static void main(
            String[] args) {

        // ----------------------------------------------------
        // Database
        // ----------------------------------------------------

        DatabaseManager databaseManager =
                new DatabaseManager(
                        DATABASE_URL
                );

        databaseManager.initialize();

        // ----------------------------------------------------
        // Repositories
        // ----------------------------------------------------

        UserRepository userRepository =
                new SQLiteUserRepository(
                        databaseManager
                );

        PortfolioRepository
                portfolioRepository =
                new SQLitePortfolioRepository(
                        databaseManager
                );

        WatchlistRepository
                watchlistRepository =
                new SQLiteWatchlistRepository(
                        databaseManager
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
        // Application Services
        // ----------------------------------------------------

        PortfolioService portfolioService =
                new PortfolioService(
                        portfolioRepository
                );

        WatchlistService watchlistService =
                new WatchlistService(
                        watchlistRepository
                );

        RequestValidator requestValidator =
                new RequestValidator();

        ExposureCalculator exposureCalculator =
                new ExposureCalculator();

        SecurityComparisonCalculator
                securityComparisonCalculator =
                new SecurityComparisonCalculator();

        // ----------------------------------------------------
        // Tool Manager
        // ----------------------------------------------------

        ToolManager toolManager =
                new ToolManager();

        // ----------------------------------------------------
        // External APIs
        // ----------------------------------------------------

        String alphaVantageApiKey =
                System.getenv(
                        "ALPHA_VANTAGE_API_KEY"
                );

        if (alphaVantageApiKey != null
                && !alphaVantageApiKey
                .isBlank()) {

            AlphaVantageClient
                    alphaVantageClient =
                    new AlphaVantageClient(
                            alphaVantageApiKey
                    );

            // ----------------------------------------------
            // Providers
            // ----------------------------------------------

            MarketDataProvider
                    marketDataProvider =
                    new MarketDataApiClient(
                            alphaVantageClient
                    );

            ETFDataProvider
                    etfDataProvider =
                    new ETFDataApiClient(
                            alphaVantageClient
                    );

            NewsProvider newsProvider =
                    new NewsApiClient(
                            alphaVantageClient
                    );

            // ----------------------------------------------
            // Tools
            // ----------------------------------------------

            MarketDataTool marketDataTool =
                    new MarketDataTool(
                            marketDataProvider,
                            requestValidator
                    );

            ETFExposureTool
                    etfExposureTool =
                    new ETFExposureTool(
                            etfDataProvider,
                            exposureCalculator,
                            requestValidator
                    );

            ComparisonTool comparisonTool =
                    new ComparisonTool(
                            etfDataProvider,
                            marketDataProvider,
                            exposureCalculator,
                            securityComparisonCalculator,
                            requestValidator
                    );

            NewsTool newsTool =
                    new NewsTool(
                            newsProvider,
                            requestValidator
                    );

            // ----------------------------------------------
            // Tool Registration
            // ----------------------------------------------

            toolManager.registerTool(
                    marketDataTool
            );

            toolManager.registerTool(
                    etfExposureTool
            );

            toolManager.registerTool(
                    comparisonTool
            );

            toolManager.registerTool(
                    newsTool
            );

        } else {

            System.err.println(
                    "Warning: ALPHA_VANTAGE_API_KEY "
                            + "is not configured."
            );
        }

        // ----------------------------------------------------
        // Terminal Interface
        // ----------------------------------------------------

        CommandParser commandParser =
                new CommandParser();

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