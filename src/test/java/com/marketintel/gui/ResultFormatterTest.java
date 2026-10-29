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

        assertTrue(text.contains("Trading session: unknown"));
        assertTrue(text.contains("https://example.com/story"));
        assertTrue(text.contains("model unavailable"));
        assertTrue(text.contains("not investment advice"));
        assertFalse(text.contains("Error:"));
    }

    @Test
    void formatsToolFailuresAsErrors() {
        String text = ResultFormatter.news(ToolResult.failure("No token"));
        assertTrue(text.startsWith("Error: No token"));
    }
}
