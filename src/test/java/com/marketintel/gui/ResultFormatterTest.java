package com.marketintel.gui;

import com.marketintel.app.MovementReport;
import com.marketintel.model.ExposureResult;
import com.marketintel.model.MarketMovementContext;
import com.marketintel.model.MarketQuote;
import com.marketintel.model.NewsArticle;
import com.marketintel.model.ToolResult;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultFormatterTest {

    @Test
    void formatsAQuoteWithoutRecalculatingTheChange() {
        String text = ResultFormatter.quote(ToolResult.success(
                new MarketQuote(
                        "NVDA",
                        new BigDecimal("100.50"),
                        new BigDecimal("90"),
                        new BigDecimal("11.666")
                )
        ));

        assertTrue(text.contains("NVDA"));
        assertTrue(text.contains("100.5"));
        assertTrue(text.contains("+11.666%"));
    }

    @Test
    void formatsSectorWeightsAsPercentages() {
        String text = ResultFormatter.sectorExposure(ToolResult.success(
                new ExposureResult(
                        "QQQ",
                        Map.of("Technology", new BigDecimal("0.60"))
                )
        ));

        assertTrue(text.contains("QQQ"));
        assertTrue(text.contains("60.00%"));
        assertTrue(text.contains("provider allocation"));
    }

    @Test
    void formatsCountryExposureWithItsBasisNote() {
        String text = ResultFormatter.geographicExposure(ToolResult.success(
                new ExposureResult(
                        "SPY",
                        Map.of("United States", new BigDecimal("0.975")),
                        "Weights are the provider's reported country exposure for the fund. "
                                + "They are not a sum of the listing countries of the holdings."
                )
        ));

        assertTrue(text.contains("ETF COUNTRY EXPOSURE"));
        assertTrue(text.contains("United States"));
        assertTrue(text.contains("97.50%"));
        assertTrue(text.contains("not a sum of the listing countries"));
    }

    @Test
    void showsEvidenceAndAnUnknownTradingSessionWhenExplanationFails() {
        MarketMovementContext context = new MarketMovementContext(
                new MarketQuote(
                        "NVDA",
                        new BigDecimal("10"),
                        new BigDecimal("9"),
                        new BigDecimal("-1.5")
                ),
                List.of(new NewsArticle(
                        "Headline",
                        "https://example.com/story",
                        "Wire",
                        "2026-10-28T12:00:00Z",
                        "Summary",
                        null,
                        null
                )),
                Instant.parse("2026-10-28T16:00:00Z"),
                "Only broad sector context was available."
        );

        String text = ResultFormatter.movement(
                MovementReport.explanationFailed(context, "model unavailable")
        );

        assertTrue(text.contains("Last trade (UTC): unknown"));
        assertTrue(text.contains("https://example.com/story"));
        assertTrue(text.contains("model unavailable"));
        assertTrue(text.contains("not investment advice"));
        assertFalse(text.contains("Error:"));
    }

    @Test
    void showsTheLastTradeTimeAndMarksLaterArticles() {
        MarketMovementContext context = new MarketMovementContext(
                new MarketQuote(
                        "AAPL",
                        new BigDecimal("343.83"),
                        new BigDecimal("347.10"),
                        new BigDecimal("-0.9421"),
                        Instant.parse("2026-10-28T20:00:00Z")
                ),
                List.of(
                        new NewsArticle(
                                "Before the close",
                                "https://example.com/before",
                                "Wire",
                                "2026-10-28T18:37:53.000000Z",
                                null,
                                null,
                                null
                        ),
                        new NewsArticle(
                                "After the close",
                                "https://example.com/after",
                                "Wire",
                                "2026-10-28T21:30:00.000000Z",
                                null,
                                null,
                                null
                        )
                ),
                Instant.parse("2026-10-29T02:19:53Z"),
                null
        );

        String text = ResultFormatter.movement(
                MovementReport.explained(context, "Evidence is limited. [1]")
        );

        assertTrue(text.contains("Last trade (UTC): 2026-10-28 20:00 UTC"));
        assertTrue(text.contains("2026-10-28T21:30:00.000000Z  (after the last trade)"));
        assertFalse(text.contains("2026-10-28T18:37:53.000000Z  (after the last trade)"));
    }

    @Test
    void formatsToolFailuresAsErrors() {
        String text = ResultFormatter.news(ToolResult.failure("No token"));
        assertTrue(text.startsWith("Error: No token"));
    }
}
