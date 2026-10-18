
package com.marketintel.tools;

import com.marketintel.model.ActionType;
import com.marketintel.model.MarketMovementContext;
import com.marketintel.model.MarketQuote;
import com.marketintel.model.NewsArticle;
import com.marketintel.model.SecurityOverview;
import com.marketintel.model.ToolRequest;
import com.marketintel.model.ToolResult;

import com.marketintel.providers.MarketDataException;
import com.marketintel.providers.MarketDataProvider;
import com.marketintel.providers.NewsProvider;

import com.marketintel.services.RequestValidator;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MarketMovementToolTest {

    private final Clock fixedClock =
            Clock.fixed(
                    Instant.parse("2026-10-18T15:00:00Z"),
                    ZoneOffset.UTC
            );

    private MarketQuote sampleQuote() {

        return new MarketQuote(
                "NVDA",
                new BigDecimal("230.86"),
                new BigDecimal("228.38"),
                new BigDecimal("1.0859")
        );
    }

    private MarketDataProvider marketProvider(
            MarketQuote quote) {

        return new MarketDataProvider() {

            @Override
            public MarketQuote getQuote(String symbol) {
                return quote;
            }

            @Override
            public SecurityOverview getSecurityOverview(
                    String symbol) {

                throw new UnsupportedOperationException(
                        "Not used in this test."
                );
            }
        };
    }

    private NewsArticle sampleArticle() {

        return new NewsArticle(
                "NVIDIA AI infrastructure update",
                "https://example.com/nvda",
                "Example Financial News",
                "2026-10-18T14:00:00Z",
                "A test article about NVIDIA.",
                new BigDecimal("0.35"),
                "Bullish"
        );
    }

    private MarketMovementTool createTool(
            MarketDataProvider marketProvider,
            NewsProvider newsProvider) {

        return new MarketMovementTool(
                marketProvider,
                newsProvider,
                new RequestValidator(),
                fixedClock
        );
    }

    private ToolRequest validRequest() {

        return new ToolRequest(
                1,
                ActionType.MARKET_MOVEMENT,
                Map.of("symbol", "nvda")
        );
    }

    @Test
    void supportsMarketMovementAction() {

        MarketMovementTool tool =
                createTool(
                        marketProvider(sampleQuote()),
                        (symbol, limit) -> List.of()
                );

        assertTrue(
                tool.supports(ActionType.MARKET_MOVEMENT)
        );

        assertFalse(
                tool.supports(ActionType.FINANCIAL_NEWS)
        );
    }

    @Test
    void combinesQuoteAndNews() {

        MarketMovementTool tool =
                createTool(
                        marketProvider(sampleQuote()),
                        (symbol, limit) -> {

                            assertEquals("NVDA", symbol);
                            assertEquals(3, limit);

                            return List.of(sampleArticle());
                        }
                );

        ToolResult result =
                tool.execute(validRequest());

        assertTrue(result.isSuccess());

        assertInstanceOf(
                MarketMovementContext.class,
                result.getData()
        );

        MarketMovementContext context =
                (MarketMovementContext) result.getData();

        assertEquals(
                "NVDA",
                context.getSymbol()
        );

        assertEquals(
                new BigDecimal("1.0859"),
                context.getQuote().getPercentChange()
        );

        assertEquals(
                1,
                context.getArticles().size()
        );

        assertEquals(
                Instant.parse("2026-10-18T15:00:00Z"),
                context.getRetrievedAt()
        );

        assertNull(context.getNewsWarning());
    }

    @Test
    void rejectsInvalidSymbol() {

        MarketMovementTool tool =
                createTool(
                        marketProvider(sampleQuote()),
                        (symbol, limit) -> List.of()
                );

        ToolRequest request =
                new ToolRequest(
                        1,
                        ActionType.MARKET_MOVEMENT,
                        Map.of("symbol", "INVALID!")
                );

        ToolResult result = tool.execute(request);

        assertFalse(result.isSuccess());
    }

    @Test
    void preservesQuoteWhenNewsProviderFails() {

        NewsProvider failingNews =
                (symbol, limit) -> {
                    throw new MarketDataException(
                            "Rate limit exceeded."
                    );
                };

        MarketMovementTool tool =
                createTool(
                        marketProvider(sampleQuote()),
                        failingNews
                );

        ToolResult result =
                tool.execute(validRequest());

        assertTrue(result.isSuccess());

        MarketMovementContext context =
                (MarketMovementContext) result.getData();

        assertEquals(
                "NVDA",
                context.getQuote().getSymbol()
        );

        assertTrue(context.getArticles().isEmpty());

        assertNotNull(context.getNewsWarning());

        assertTrue(
                context.getNewsWarning()
                        .contains("Rate limit exceeded")
        );
    }

    @Test
    void failsWhenQuoteIsUnavailable() {

        MarketDataProvider failingMarket =
                new MarketDataProvider() {

                    @Override
                    public MarketQuote getQuote(
                            String symbol) {

                        throw new MarketDataException(
                                "Quote unavailable."
                        );
                    }

                    @Override
                    public SecurityOverview getSecurityOverview(
                            String symbol) {

                        throw new UnsupportedOperationException();
                    }
                };

        MarketMovementTool tool =
                createTool(
                        failingMarket,
                        (symbol, limit) -> List.of()
                );

        ToolResult result =
                tool.execute(validRequest());

        assertFalse(result.isSuccess());

        assertTrue(
                result.getErrorMessage()
                        .contains("Quote unavailable")
        );
    }

    @Test
    void contextProtectsNewsListFromModification() {

        MarketMovementContext context =
                new MarketMovementContext(
                        sampleQuote(),
                        List.of(sampleArticle()),
                        fixedClock.instant(),
                        null
                );

        assertThrows(
                UnsupportedOperationException.class,
                () -> context.getArticles().clear()
        );
    }
}
