
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
import com.marketintel.providers.MarketauxNewsClient;
import com.marketintel.providers.NewsProvider;

import com.marketintel.services.ExposureCalculator;
import com.marketintel.services.PortfolioService;
import com.marketintel.services.RequestValidator;
import com.marketintel.services.SecurityComparisonCalculator;
import com.marketintel.services.WatchlistService;

import com.marketintel.tools.ComparisonTool;
import com.marketintel.tools.ETFExposureTool;
import com.marketintel.tools.MarketDataTool;
import com.marketintel.tools.MarketMovementTool;
import com.marketintel.tools.NewsTool;

import com.marketintel.ui.CommandParser;
import com.marketintel.ui.TerminalUI;
import com.marketintel.ui.UserInterface;

import com.marketintel.providers.GeminiLLMClient;
import com.marketintel.providers.LLMProvider;
import com.marketintel.services.MarketExplanationService;

public class Main {

    private static final String DATABASE_URL =
            "jdbc:sqlite:data/market-intelligence.db";

    public static void main(String[] args) {

        // --------------------------------------------------
        // Database
        // --------------------------------------------------

        DatabaseManager databaseManager =
                new DatabaseManager(DATABASE_URL);

        databaseManager.initialize();

        UserRepository userRepository =
                new SQLiteUserRepository(databaseManager);

        PortfolioRepository portfolioRepository =
                new SQLitePortfolioRepository(databaseManager);

        WatchlistRepository watchlistRepository =
                new SQLiteWatchlistRepository(databaseManager);

        // --------------------------------------------------
        // Authentication
        // --------------------------------------------------

        PasswordHasher passwordHasher =
                new BCryptPasswordHasher();

        AuthService authService =
                new AuthService(
                        userRepository,
                        passwordHasher
                );

        SessionManager sessionManager =
                new SessionManager();

        // --------------------------------------------------
        // Services
        // --------------------------------------------------

        PortfolioService portfolioService =
                new PortfolioService(portfolioRepository);

        WatchlistService watchlistService =
                new WatchlistService(watchlistRepository);

        RequestValidator requestValidator =
                new RequestValidator();

        ExposureCalculator exposureCalculator =
                new ExposureCalculator();

        SecurityComparisonCalculator
                securityComparisonCalculator =
                new SecurityComparisonCalculator();

        ToolManager toolManager =
                new ToolManager();

        // --------------------------------------------------
        // Provider references
        // --------------------------------------------------

        MarketDataProvider marketDataProvider = null;
        NewsProvider newsProvider = null;

        // --------------------------------------------------
        // Alpha Vantage: Quotes and ETF Data
        // --------------------------------------------------

        String alphaVantageApiKey =
                System.getenv("ALPHA_VANTAGE_API_KEY");

        if (alphaVantageApiKey != null
                && !alphaVantageApiKey.isBlank()) {

            AlphaVantageClient alphaVantageClient =
                    new AlphaVantageClient(
                            alphaVantageApiKey
                    );

            marketDataProvider =
                    new MarketDataApiClient(
                            alphaVantageClient
                    );

            ETFDataProvider etfDataProvider =
                    new ETFDataApiClient(
                            alphaVantageClient
                    );

            toolManager.registerTool(
                    new MarketDataTool(
                            marketDataProvider,
                            requestValidator
                    )
            );

            toolManager.registerTool(
                    new ETFExposureTool(
                            etfDataProvider,
                            exposureCalculator,
                            requestValidator
                    )
            );

            toolManager.registerTool(
                    new ComparisonTool(
                            etfDataProvider,
                            marketDataProvider,
                            exposureCalculator,
                            securityComparisonCalculator,
                            requestValidator
                    )
            );

        } else {

            System.err.println(
                    "Warning: ALPHA_VANTAGE_API_KEY "
                            + "is not configured."
            );
        }

        // --------------------------------------------------
        // Marketaux: Financial News
        // --------------------------------------------------

        String marketauxToken =
                System.getenv("MARKETAUX_API_TOKEN");

        if (marketauxToken != null
                && !marketauxToken.isBlank()) {

            newsProvider =
                    new MarketauxNewsClient(
                            marketauxToken
                    );

            toolManager.registerTool(
                    new NewsTool(
                            newsProvider,
                            requestValidator
                    )
            );

        } else {

            System.err.println(
                    "Warning: MARKETAUX_API_TOKEN "
                            + "is not configured."
            );
        }

        // --------------------------------------------------
        // Combined Market Movement Tool
        // --------------------------------------------------

        if (marketDataProvider != null
                && newsProvider != null) {

            toolManager.registerTool(
                    new MarketMovementTool(
                            marketDataProvider,
                            newsProvider,
                            requestValidator
                    )
            );

        } else {

            System.err.println(
                    "Warning: Market movement analysis "
                            + "requires both API providers."
            );
        }

        // --------------------------------------------------
        // Gemini: Market Explanation Service
        // --------------------------------------------------

        MarketExplanationService marketExplanationService = null;

        String geminiApiKey =
                System.getenv("GEMINI_API_KEY");

        if (geminiApiKey != null
                && !geminiApiKey.isBlank()) {

            String geminiModel =
                    System.getenv("GEMINI_MODEL");

            LLMProvider llmProvider;

            if (geminiModel == null
                    || geminiModel.isBlank()) {

                llmProvider =
                        new GeminiLLMClient(geminiApiKey);

            } else {

                llmProvider =
                        new GeminiLLMClient(
                                geminiApiKey,
                                geminiModel.trim()
                        );
            }

            marketExplanationService =
                    new MarketExplanationService(llmProvider);

        } else {

            System.err.println(
                    "Warning: GEMINI_API_KEY is not configured. "
                            + "AI explanations will be unavailable."
            );
        }

        // --------------------------------------------------
        // Terminal
        // --------------------------------------------------

        CommandParser commandParser =
                new CommandParser();

        UserInterface userInterface =
                new TerminalUI(
                        authService,
                        sessionManager,
                        commandParser,
                        portfolioService,
                        watchlistService,
                        toolManager,
                        marketExplanationService
                );

        userInterface.start();
    }
}
