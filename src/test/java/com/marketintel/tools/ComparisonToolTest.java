package com.marketintel.tools;

import com.marketintel.model.ActionType;
import com.marketintel.model.ETFHolding;
import com.marketintel.model.ETFProfile;
import com.marketintel.model.MarketQuote;
import com.marketintel.model.OverlapResult;
import com.marketintel.model.SecurityComparisonResult;
import com.marketintel.model.SecurityOverview;
import com.marketintel.model.ToolRequest;
import com.marketintel.model.ToolResult;

import com.marketintel.providers.ETFDataProvider;
import com.marketintel.providers.MarketDataProvider;

import com.marketintel.services.ExposureCalculator;
import com.marketintel.services.RequestValidator;
import com.marketintel.services.SecurityComparisonCalculator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ComparisonToolTest {

    private ComparisonTool tool;

    @BeforeEach
    void setUp() {

        tool =
                new ComparisonTool(
                        new FakeETFDataProvider(),
                        new FakeMarketDataProvider(),
                        new ExposureCalculator(),
                        new SecurityComparisonCalculator(),
                        new RequestValidator()
                );
    }

    @Test
    void supportsEtfOverlap() {

        assertTrue(
                tool.supports(
                        ActionType.ETF_OVERLAP
                )
        );
    }

    @Test
    void supportsSecurityComparison() {

        assertTrue(
                tool.supports(
                        ActionType
                                .SECURITY_COMPARISON
                )
        );
    }

    @Test
    void calculatesEtfOverlap() {

        ToolRequest request =
                new ToolRequest(
                        1,
                        ActionType.ETF_OVERLAP,

                        Map.of(
                                "firstSymbol",
                                "ETF1",

                                "secondSymbol",
                                "ETF2"
                        )
                );

        ToolResult result =
                tool.execute(
                        request
                );

        assertTrue(
                result.isSuccess()
        );

        assertInstanceOf(
                OverlapResult.class,
                result.getData()
        );

        OverlapResult overlap =
                (OverlapResult)
                        result.getData();

        assertEquals(
                new BigDecimal(
                        "0.06"
                ),
                overlap.getOverlapWeight()
        );

        assertEquals(
                1,
                overlap
                        .getCommonHoldings()
                        .size()
        );

        assertEquals(
                "NVDA",
                overlap
                        .getCommonHoldings()
                        .getFirst()
                        .getSymbol()
        );
    }

    @Test
    void comparesSecurityQuotes() {

        ToolRequest request =
                new ToolRequest(
                        1,
                        ActionType
                                .SECURITY_COMPARISON,

                        Map.of(
                                "firstSymbol",
                                "AAA",

                                "secondSymbol",
                                "BBB"
                        )
                );

        ToolResult result =
                tool.execute(
                        request
                );

        assertTrue(
                result.isSuccess()
        );

        assertInstanceOf(
                SecurityComparisonResult.class,
                result.getData()
        );

        SecurityComparisonResult comparison =
                (SecurityComparisonResult)
                        result.getData();

        assertEquals(
                "AAA",
                comparison
                        .getFirstQuote()
                        .getSymbol()
        );

        assertEquals(
                "BBB",
                comparison
                        .getSecondQuote()
                        .getSymbol()
        );

        assertEquals(
                new BigDecimal(
                        "3.50"
                ),
                comparison
                        .getPercentChangeDifference()
        );
    }

    @Test
    void missingSecondSymbolIsRejected() {

        ToolRequest request =
                new ToolRequest(
                        1,
                        ActionType
                                .SECURITY_COMPARISON,

                        Map.of(
                                "firstSymbol",
                                "AAA"
                        )
                );

        ToolResult result =
                tool.execute(
                        request
                );

        assertFalse(
                result.isSuccess()
        );
    }

    @Test
    void comparingSameSymbolIsRejected() {

        ToolRequest request =
                new ToolRequest(
                        1,
                        ActionType
                                .SECURITY_COMPARISON,

                        Map.of(
                                "firstSymbol",
                                "NVDA",

                                "secondSymbol",
                                "nvda"
                        )
                );

        ToolResult result =
                tool.execute(
                        request
                );

        assertFalse(
                result.isSuccess()
        );
    }

    private static class FakeETFDataProvider
            implements ETFDataProvider {

        @Override
        public ETFProfile getProfile(
                String symbol) {

            if (symbol.equalsIgnoreCase(
                    "ETF1")) {

                return new ETFProfile(
                        "ETF1",
                        null,
                        null,

                        List.of(
                                new ETFHolding(
                                        "NVDA",
                                        "NVIDIA",
                                        new BigDecimal(
                                                "0.10"
                                        )
                                ),

                                new ETFHolding(
                                        "MSFT",
                                        "Microsoft",
                                        new BigDecimal(
                                                "0.08"
                                        )
                                )
                        ),

                        Map.of()
                );
            }

            return new ETFProfile(
                    "ETF2",
                    null,
                    null,

                    List.of(
                            new ETFHolding(
                                    "NVDA",
                                    "NVIDIA",
                                    new BigDecimal(
                                            "0.06"
                                    )
                            ),

                            new ETFHolding(
                                    "AAPL",
                                    "Apple",
                                    new BigDecimal(
                                            "0.07"
                                    )
                            )
                    ),

                    Map.of()
            );
        }
    }

    private static class FakeMarketDataProvider
            implements MarketDataProvider {

        @Override
        public MarketQuote getQuote(
                String symbol) {

            if (symbol.equalsIgnoreCase(
                    "AAA")) {

                return new MarketQuote(
                        "AAA",
                        new BigDecimal(
                                "100"
                        ),
                        new BigDecimal(
                                "97.50"
                        ),
                        new BigDecimal(
                                "2.50"
                        )
                );
            }

            return new MarketQuote(
                    symbol.toUpperCase(),
                    new BigDecimal(
                            "200"
                    ),
                    new BigDecimal(
                            "202"
                    ),
                    new BigDecimal(
                            "-1.00"
                    )
            );
        }

        @Override
        public SecurityOverview
        getSecurityOverview(
                String symbol) {

            throw new UnsupportedOperationException(
                    "Not required for this test."
            );
        }
    }
}