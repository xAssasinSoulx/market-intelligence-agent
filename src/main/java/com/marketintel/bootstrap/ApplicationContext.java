package com.marketintel.bootstrap;

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
import com.marketintel.providers.FinnhubMarketDataClient;
import com.marketintel.providers.GeminiLLMClient;
import com.marketintel.providers.LLMProvider;
import com.marketintel.providers.MarketDataApiClient;
import com.marketintel.providers.MarketDataProvider;
import com.marketintel.providers.MarketauxNewsClient;
import com.marketintel.providers.NewsProvider;
import com.marketintel.providers.UnconfiguredETFDataProvider;
import com.marketintel.services.ExposureCalculator;
import com.marketintel.services.MarketExplanationService;
import com.marketintel.services.PortfolioExposureCalculator;
import com.marketintel.services.PortfolioService;
import com.marketintel.services.RequestValidator;
import com.marketintel.services.SecurityComparisonCalculator;
import com.marketintel.services.WatchlistService;
import com.marketintel.tools.PortfolioAnalysisTool;
import com.marketintel.tools.ComparisonTool;
import com.marketintel.tools.ETFExposureTool;
import com.marketintel.tools.MarketDataTool;
import com.marketintel.tools.MarketMovementTool;
import com.marketintel.tools.NewsTool;
import com.marketintel.ui.CommandParser;
import com.marketintel.ui.TerminalUI;
import com.marketintel.ui.UserInterface;

/**
 * Builds the services shared by the terminal and the graphical interface.
 * Provider clients are registered only when the corresponding environment
 * variable is present. Missing keys leave that capability unavailable.
 */
public final class ApplicationContext {

    public static final String DEFAULT_DATABASE_URL =
            "jdbc:sqlite:data/market-intelligence.db";

    private final AuthService authService;
    private final SessionManager sessionManager;
    private final PortfolioService portfolioService;
    private final WatchlistService watchlistService;
    private final ToolManager toolManager;
    private final MarketExplanationService marketExplanationService;
    private final CommandParser commandParser;

    private ApplicationContext(
            AuthService authService,
            SessionManager sessionManager,
            PortfolioService portfolioService,
            WatchlistService watchlistService,
            ToolManager toolManager,
            MarketExplanationService marketExplanationService,
            CommandParser commandParser) {

        this.authService = authService;
        this.sessionManager = sessionManager;
        this.portfolioService = portfolioService;
        this.watchlistService = watchlistService;
        this.toolManager = toolManager;
        this.marketExplanationService = marketExplanationService;
        this.commandParser = commandParser;
    }

    public static ApplicationContext createDefault() {
        return create(DEFAULT_DATABASE_URL);
    }

    public static ApplicationContext create(String databaseUrl) {

        DatabaseManager databaseManager =
                new DatabaseManager(databaseUrl);

        databaseManager.initialize();

        UserRepository userRepository =
                new SQLiteUserRepository(databaseManager);

        PortfolioRepository portfolioRepository =
                new SQLitePortfolioRepository(databaseManager);

        WatchlistRepository watchlistRepository =
                new SQLiteWatchlistRepository(databaseManager);

        PasswordHasher passwordHasher = new BCryptPasswordHasher();

        AuthService authService =
                new AuthService(userRepository, passwordHasher);

        SessionManager sessionManager = new SessionManager();

        PortfolioService portfolioService =
                new PortfolioService(portfolioRepository);

        WatchlistService watchlistService =
                new WatchlistService(watchlistRepository);

        RequestValidator requestValidator = new RequestValidator();

        ExposureCalculator exposureCalculator =
                new ExposureCalculator();

        SecurityComparisonCalculator securityComparisonCalculator =
                new SecurityComparisonCalculator();

        ToolManager toolManager = new ToolManager();

        MarketDataProvider marketDataProvider = null;
        ETFDataProvider etfDataProvider = null;
        NewsProvider newsProvider = null;

        String finnhubApiKey = System.getenv("FINNHUB_API_KEY");
        String alphaVantageApiKey = System.getenv("ALPHA_VANTAGE_API_KEY");

        AlphaVantageClient alphaVantageClient = null;
        if (alphaVantageApiKey != null && !alphaVantageApiKey.isBlank()) {
            alphaVantageClient = new AlphaVantageClient(alphaVantageApiKey);
            etfDataProvider = new ETFDataApiClient(alphaVantageClient);
        }

        if (finnhubApiKey != null && !finnhubApiKey.isBlank()) {
            marketDataProvider = new FinnhubMarketDataClient(finnhubApiKey);
        } else if (alphaVantageClient != null) {
            marketDataProvider = new MarketDataApiClient(alphaVantageClient);
            System.err.println(
                    "Warning: FINNHUB_API_KEY is not configured. Quotes use Alpha Vantage, "
                            + "whose company overview is a premium endpoint."
            );
        } else {
            System.err.println(
                    "Warning: Neither FINNHUB_API_KEY nor ALPHA_VANTAGE_API_KEY is configured."
            );
        }

        if (etfDataProvider == null) {
            System.err.println(
                    "Warning: ALPHA_VANTAGE_API_KEY is not configured. ETF holdings are unavailable."
            );
        }

        if (marketDataProvider != null) {

            ETFDataProvider etfForTools = etfDataProvider != null
                    ? etfDataProvider
                    : new UnconfiguredETFDataProvider();

            toolManager.registerTool(
                    new MarketDataTool(
                            marketDataProvider,
                            requestValidator
                    )
            );

            toolManager.registerTool(
                    new ComparisonTool(
                            etfForTools,
                            marketDataProvider,
                            exposureCalculator,
                            securityComparisonCalculator,
                            requestValidator
                    )
            );

            toolManager.registerTool(
                    new PortfolioAnalysisTool(
                            portfolioService,
                            marketDataProvider,
                            etfForTools,
                            new PortfolioExposureCalculator(),
                            requestValidator
                    )
            );
        }

        if (etfDataProvider != null) {

            toolManager.registerTool(
                    new ETFExposureTool(
                            etfDataProvider,
                            exposureCalculator,
                            requestValidator
                    )
            );
        }

        String marketauxToken =
                System.getenv("MARKETAUX_API_TOKEN");

        if (marketauxToken != null && !marketauxToken.isBlank()) {

            newsProvider = new MarketauxNewsClient(marketauxToken);

            toolManager.registerTool(
                    new NewsTool(newsProvider, requestValidator)
            );

        } else {

            System.err.println(
                    "Warning: MARKETAUX_API_TOKEN is not configured."
            );
        }

        if (marketDataProvider != null && newsProvider != null) {

            toolManager.registerTool(
                    new MarketMovementTool(
                            marketDataProvider,
                            newsProvider,
                            requestValidator
                    )
            );

        } else {

            System.err.println(
                    "Warning: Market movement analysis requires both API providers."
            );
        }

        MarketExplanationService marketExplanationService = null;

        String geminiApiKey = System.getenv("GEMINI_API_KEY");

        if (geminiApiKey != null && !geminiApiKey.isBlank()) {

            String geminiModel = System.getenv("GEMINI_MODEL");

            LLMProvider llmProvider;

            if (geminiModel == null || geminiModel.isBlank()) {
                llmProvider = new GeminiLLMClient(geminiApiKey);
            } else {
                llmProvider = new GeminiLLMClient(
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

        return new ApplicationContext(
                authService,
                sessionManager,
                portfolioService,
                watchlistService,
                toolManager,
                marketExplanationService,
                new CommandParser()
        );
    }

    public AuthService getAuthService() {
        return authService;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public PortfolioService getPortfolioService() {
        return portfolioService;
    }

    public WatchlistService getWatchlistService() {
        return watchlistService;
    }

    public ToolManager getToolManager() {
        return toolManager;
    }

    public MarketExplanationService getMarketExplanationService() {
        return marketExplanationService;
    }

    public boolean isExplanationAvailable() {
        return marketExplanationService != null;
    }

    public UserInterface createTerminalInterface() {

        return new TerminalUI(
                authService,
                sessionManager,
                commandParser,
                portfolioService,
                watchlistService,
                toolManager,
                marketExplanationService
        );
    }
}
