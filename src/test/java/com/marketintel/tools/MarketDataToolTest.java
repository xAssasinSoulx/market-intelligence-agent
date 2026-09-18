package com.marketintel.tools;

import com.marketintel.model.ActionType;
import com.marketintel.model.MarketQuote;
import com.marketintel.model.SecurityOverview;
import com.marketintel.model.ToolRequest;
import com.marketintel.model.ToolResult;

import com.marketintel.providers.MarketDataProvider;

import com.marketintel.services.RequestValidator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MarketDataToolTest {

    private MarketDataTool tool;

    @BeforeEach
    void setUp() {

        MarketDataProvider provider =
                new FakeMarketDataProvider();

        tool =
                new MarketDataTool(
                        provider,
                        new RequestValidator()
                );
    }

    @Test
    void supportsMarketQuote() {

        assertTrue(
                tool.supports(
                        ActionType.MARKET_QUOTE
                )
        );
    }

    @Test
    void supportsSecurityOverview() {

        assertTrue(
                tool.supports(
                        ActionType.SECURITY_OVERVIEW
                )
        );
    }

    @Test
    void returnsMarketQuote() {

        ToolRequest request =
                new ToolRequest(
                        1,
                        ActionType.MARKET_QUOTE,
                        Map.of(
                                "symbol",
                                "NVDA"
                        )
                );

        ToolResult result =
                tool.execute(request);

        assertTrue(
                result.isSuccess()
        );

        assertInstanceOf(
                MarketQuote.class,
                result.getData()
        );
    }

    @Test
    void returnsSecurityOverview() {

        ToolRequest request =
                new ToolRequest(
                        1,
                        ActionType.SECURITY_OVERVIEW,
                        Map.of(
                                "symbol",
                                "NVDA"
                        )
                );

        ToolResult result =
                tool.execute(request);

        assertTrue(
                result.isSuccess()
        );

        assertInstanceOf(
                SecurityOverview.class,
                result.getData()
        );
    }

    @Test
    void rejectsMissingSymbol() {

        ToolRequest request =
                new ToolRequest(
                        1,
                        ActionType.MARKET_QUOTE,
                        Map.of()
                );

        ToolResult result =
                tool.execute(request);

        assertFalse(
                result.isSuccess()
        );
    }

    private static class FakeMarketDataProvider
            implements MarketDataProvider {

        @Override
        public MarketQuote getQuote(
                String symbol) {

            return new MarketQuote(
                    symbol,
                    new BigDecimal("219.34"),
                    new BigDecimal("213.90"),
                    new BigDecimal("2.5432")
            );
        }

        @Override
        public SecurityOverview
        getSecurityOverview(
                String symbol) {

            return new SecurityOverview(
                    symbol,
                    "NVIDIA Corporation",
                    "Test description",
                    "NASDAQ",
                    "USD",
                    "Technology",
                    "Semiconductors",
                    new BigDecimal(
                            "5000000000000"
                    )
            );
        }
    }
}