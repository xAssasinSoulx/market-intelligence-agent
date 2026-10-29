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
import com.marketintel.providers.AlphaVantageAdapter;
import com.marketintel.providers.AlphaVantageClient;
import com.marketintel.providers.DataCapability;
import com.marketintel.providers.ETFDataApiClient;
import com.marketintel.providers.ETFDataProvider;
import com.marketintel.providers.FinnhubAdapter;
import com.marketintel.providers.FinnhubMarketDataClient;
import com.marketintel.providers.FinancialModelingPrepAdapter;
import com.marketintel.providers.GeminiLLMClient;
import com.marketintel.providers.LLMProvider;
import com.marketintel.providers.MarketDataApiClient;
import com.marketintel.providers.MarketDataProvider;
import com.marketintel.providers.MarketauxAdapter;
import com.marketintel.providers.MarketauxNewsClient;
import com.marketintel.providers.NewsApiClient;
import com.marketintel.providers.NewsProvider;
import com.marketintel.providers.ProviderRouter;
import com.marketintel.providers.SentiSenseAdapter;
import com.marketintel.providers.TwelveDataAdapter;
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
import com.marketintel.tools.MarketInsightTool;
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

        /*
         * Registration order is the priority for that capability.
         * A later adapter is used only when an earlier one is absent.
         * Alpha Vantage can serve news, and that capability is left
         * unregistered: the free plan is about 25 calls a day, and news
         * is covered by Marketaux or Finnhub.
         */
        ProviderRouter router = new ProviderRouter();

        String finnhubApiKey = System.getenv("FINNHUB_API_KEY");
        String alphaVantageApiKey = System.getenv("ALPHA_VANTAGE_API_KEY");
        String twelveDataApiKey = System.getenv("TWELVE_DATA_API_KEY");
        String fmpApiKey = System.getenv("FMP_API_KEY");
        String sentiSenseApiKey = System.getenv("SENTISENSE_API_KEY");
        String marketauxToken = System.getenv("MARKETAUX_API_TOKEN");

        FinnhubMarketDataClient finnhubClient = null;
        FinnhubAdapter finnhubAdapter = null;
        boolean alphaConfigured = false;

        if (configured(finnhubApiKey)) {
            finnhubClient = new FinnhubMarketDataClient(finnhubApiKey);
            finnhubAdapter = new FinnhubAdapter(finnhubClient);
            router.prefer(DataCapability.QUOTE, finnhubAdapter);
            router.prefer(DataCapability.OVERVIEW, finnhubAdapter);
        }

        if (configured(twelveDataApiKey)) {
            TwelveDataAdapter twelveData = new TwelveDataAdapter(twelveDataApiKey);
            router.prefer(DataCapability.QUOTE, twelveData);
            router.prefer(DataCapability.PRICE_HISTORY, twelveData);
        }

        if (configured(alphaVantageApiKey)) {
            alphaConfigured = true;
            AlphaVantageClient alphaVantageClient = new AlphaVantageClient(alphaVantageApiKey);
            AlphaVantageAdapter alphaVantage = new AlphaVantageAdapter(
                    new MarketDataApiClient(alphaVantageClient),
                    new ETFDataApiClient(alphaVantageClient),
                    new NewsApiClient(alphaVantageClient)
            );
            router.prefer(DataCapability.QUOTE, alphaVantage);
            router.prefer(DataCapability.OVERVIEW, alphaVantage);
            router.prefer(DataCapability.ETF_PROFILE, alphaVantage);
        }

        if (configured(marketauxToken)) {
            router.prefer(
                    DataCapability.NEWS,
                    new MarketauxAdapter(new MarketauxNewsClient(marketauxToken))
            );
        }
        if (finnhubAdapter != null) {
            router.prefer(DataCapability.NEWS, finnhubAdapter);
        }

        if (configured(fmpApiKey)) {
            router.prefer(
                    DataCapability.FINANCIAL_STATEMENTS,
                    new FinancialModelingPrepAdapter(fmpApiKey)
            );
        }
        if (configured(sentiSenseApiKey)) {
            router.prefer(DataCapability.SENTIMENT, new SentiSenseAdapter(sentiSenseApiKey));
        }

        warnForMissingProviders(router, finnhubAdapter != null, alphaConfigured);

        MarketDataProvider marketDataProvider = router.serves(DataCapability.QUOTE)
                ? router
                : null;
        ETFDataProvider etfDataProvider = router.serves(DataCapability.ETF_PROFILE)
                ? router
                : null;
        NewsProvider newsProvider = router.serves(DataCapability.NEWS)
                ? router
                : null;

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

        if (etfDataProvider != null || finnhubClient != null) {

            toolManager.registerTool(
                    new ETFExposureTool(
                            etfDataProvider != null
                                    ? etfDataProvider
                                    : new UnconfiguredETFDataProvider(),
                            exposureCalculator,
                            requestValidator,
                            finnhubClient,
                            etfDataProvider != null,
                            finnhubClient
                    )
            );
        }

        if (newsProvider != null) {
            toolManager.registerTool(
                    new NewsTool(newsProvider, requestValidator)
            );
        }

        if (router.serves(DataCapability.FINANCIAL_STATEMENTS)
                || router.serves(DataCapability.PRICE_HISTORY)
                || router.serves(DataCapability.SENTIMENT)) {

            toolManager.registerTool(
                    new MarketInsightTool(
                            router.serves(DataCapability.FINANCIAL_STATEMENTS) ? router : null,
                            router.serves(DataCapability.PRICE_HISTORY) ? router : null,
                            router.serves(DataCapability.SENTIMENT) ? router : null,
                            requestValidator
                    )
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

    private static boolean configured(String value) {
        return value != null && !value.isBlank();
    }

    private static void warnForMissingProviders(
            ProviderRouter router,
            boolean finnhubConfigured,
            boolean alphaConfigured) {

        if (!router.serves(DataCapability.QUOTE)) {
            System.err.println(
                    "Warning: No quote provider is configured. "
                            + "Set FINNHUB_API_KEY, TWELVE_DATA_API_KEY, or ALPHA_VANTAGE_API_KEY."
            );
        } else if (!finnhubConfigured && alphaConfigured) {
            System.err.println(
                    "Warning: FINNHUB_API_KEY is not configured. "
                            + "Quotes use the next registered adapter. "
                            + "Alpha Vantage company overview is a premium endpoint."
            );
        } else if (!finnhubConfigured) {
            System.err.println(
                    "Warning: FINNHUB_API_KEY is not configured. "
                            + "Company overviews need Finnhub or Alpha Vantage."
            );
        }

        if (!router.serves(DataCapability.ETF_PROFILE)) {
            System.err.println(
                    "Warning: ALPHA_VANTAGE_API_KEY is not configured. ETF holdings are unavailable."
            );
        }
        if (!router.serves(DataCapability.NEWS)) {
            System.err.println(
                    "Warning: No news provider is configured. "
                            + "Set MARKETAUX_API_TOKEN or FINNHUB_API_KEY."
            );
        }
        if (!router.serves(DataCapability.FINANCIAL_STATEMENTS)) {
            System.err.println(
                    "Warning: FMP_API_KEY is not configured. Financial statements are unavailable."
            );
        }
        if (!router.serves(DataCapability.PRICE_HISTORY)) {
            System.err.println(
                    "Warning: TWELVE_DATA_API_KEY is not configured. Price history is unavailable."
            );
        }
        if (!router.serves(DataCapability.SENTIMENT)) {
            System.err.println(
                    "Warning: SENTISENSE_API_KEY is not configured. "
                            + "News sentiment scores are unavailable."
            );
        }
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
