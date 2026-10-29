package com.marketintel.tools;

import com.marketintel.auth.AuthService;
import com.marketintel.auth.BCryptPasswordHasher;
import com.marketintel.model.ActionType;
import com.marketintel.model.AssetType;
import com.marketintel.model.ETFHolding;
import com.marketintel.model.ETFProfile;
import com.marketintel.model.MarketQuote;
import com.marketintel.model.PortfolioExposureResult;
import com.marketintel.model.SecurityOverview;
import com.marketintel.model.ToolRequest;
import com.marketintel.model.ToolResult;
import com.marketintel.persistence.DatabaseManager;
import com.marketintel.persistence.SQLitePortfolioRepository;
import com.marketintel.persistence.SQLiteUserRepository;
import com.marketintel.providers.ETFDataProvider;
import com.marketintel.providers.MarketDataException;
import com.marketintel.providers.MarketDataProvider;
import com.marketintel.services.PortfolioExposureCalculator;
import com.marketintel.services.PortfolioService;
import com.marketintel.services.RequestValidator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortfolioAnalysisToolTest {

    @TempDir
    Path tempDirectory;

    private PortfolioService portfolioService;
    private long firstUser;
    private long secondUser;
    private PortfolioAnalysisTool tool;

    @BeforeEach
    void setUp() {
        DatabaseManager databaseManager = new DatabaseManager(
                "jdbc:sqlite:" + tempDirectory.resolve("exposure.db")
        );
        databaseManager.initialize();
        AuthService authService = new AuthService(
                new SQLiteUserRepository(databaseManager),
                new BCryptPasswordHasher()
        );
        firstUser = authService.register("ada", "SecurePassword123").getId();
        secondUser = authService.register("grace", "SecurePassword123").getId();
        portfolioService = new PortfolioService(new SQLitePortfolioRepository(databaseManager));
        tool = new PortfolioAnalysisTool(
                portfolioService,
                new FixedMarketData(),
                new FixedEtfData(),
                new PortfolioExposureCalculator(),
                new RequestValidator()
        );
    }

    @Test
    void analyzesOnlyTheRequestedUsersPositions() {
        portfolioService.addPosition(firstUser, "NVDA", new BigDecimal("2"), AssetType.STOCK);
        portfolioService.addPosition(secondUser, "MSFT", new BigDecimal("9"), AssetType.STOCK);

        ToolResult result = tool.execute(new ToolRequest(
                firstUser,
                ActionType.PORTFOLIO_EXPOSURE,
                Map.of()
        ));

        PortfolioExposureResult exposure = (PortfolioExposureResult) result.getData();
        assertTrue(result.isSuccess());
        assertEquals("NVDA", exposure.getPositions().getFirst().getSymbol());
        assertEquals(1, exposure.getPositions().size());
        assertEquals(0, new BigDecimal("200").compareTo(
                exposure.getBooks().getFirst().getTotalMarketValue()
        ));
    }

    @Test
    void recordsAnEtfWhoseHoldingsCannotBeLoaded() {
        portfolioService.addPosition(firstUser, "QQQ", new BigDecimal("1"), AssetType.ETF);

        PortfolioAnalysisTool failingEtfs = new PortfolioAnalysisTool(
                portfolioService,
                new FixedMarketData(),
                symbol -> {
                    throw new MarketDataException("holdings endpoint unavailable");
                },
                new PortfolioExposureCalculator(),
                new RequestValidator()
        );

        ToolResult result = failingEtfs.execute(new ToolRequest(
                firstUser,
                ActionType.PORTFOLIO_EXPOSURE,
                Map.of()
        ));

        PortfolioExposureResult exposure = (PortfolioExposureResult) result.getData();
        assertTrue(result.isSuccess());
        assertTrue(exposure.getLimitations().stream().anyMatch(note -> note.contains("unavailable")));
        assertEquals(0, BigDecimal.ONE.compareTo(
                exposure.getBooks().getFirst().getSectorWeights().get("Unknown")
        ));
    }

    @Test
    void keepsAnEtfInTheProvidersMarketWhenItHasNoCompanyProfile() {
        portfolioService.addPosition(firstUser, "AAPL", new BigDecimal("1"), AssetType.STOCK);
        portfolioService.addPosition(firstUser, "QQQ", new BigDecimal("1"), AssetType.ETF);

        PortfolioAnalysisTool singleMarket = new PortfolioAnalysisTool(
                portfolioService,
                new UsOnlyMarketData(),
                new FixedEtfData(),
                new PortfolioExposureCalculator(),
                new RequestValidator()
        );

        ToolResult result = singleMarket.execute(new ToolRequest(
                firstUser,
                ActionType.PORTFOLIO_EXPOSURE,
                Map.of()
        ));

        PortfolioExposureResult exposure = (PortfolioExposureResult) result.getData();
        assertEquals(1, exposure.getBooks().size());
        assertEquals("USD", exposure.getBooks().getFirst().getCurrency());
        assertEquals(0, new BigDecimal("200").compareTo(
                exposure.getBooks().getFirst().getTotalMarketValue()
        ));
        assertTrue(exposure.getLimitations().stream().anyMatch(note ->
                note.contains("QQQ") && note.contains("USD")));
    }

    private static final class UsOnlyMarketData extends FixedMarketData {

        @Override
        public SecurityOverview getSecurityOverview(String symbol) {
            if ("QQQ".equals(symbol)) {
                throw new MarketDataException("No security overview found for symbol: QQQ");
            }
            return super.getSecurityOverview(symbol);
        }

        @Override
        public java.util.Optional<String> quoteCurrency() {
            return java.util.Optional.of("USD");
        }
    }

    private static class FixedMarketData implements MarketDataProvider {

        @Override
        public MarketQuote getQuote(String symbol) {
            return new MarketQuote(
                    symbol,
                    new BigDecimal("100"),
                    new BigDecimal("90"),
                    new BigDecimal("11")
            );
        }

        @Override
        public SecurityOverview getSecurityOverview(String symbol) {
            return new SecurityOverview(
                    symbol,
                    symbol,
                    null,
                    "NASDAQ",
                    "USD",
                    "Technology",
                    "Semiconductors",
                    null
            );
        }
    }

    private static final class FixedEtfData implements ETFDataProvider {

        @Override
        public ETFProfile getProfile(String symbol) {
            return new ETFProfile(
                    symbol,
                    null,
                    null,
                    List.of(new ETFHolding("AAPL", "Apple", new BigDecimal("0.50"))),
                    Map.of("Technology", BigDecimal.ONE)
            );
        }
    }
}
