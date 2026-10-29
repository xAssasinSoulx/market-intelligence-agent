package com.marketintel.gui;

import com.marketintel.model.MarketQuote;
import com.marketintel.model.NewsArticle;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TapeLineTest {

    @Test
    void joinsQuotesAndHeadlinesWithoutRecalculatingTheChange() {
        String line = TapeLine.format(
                List.of(new MarketQuote(
                        "SPY",
                        new BigDecimal("500.5"),
                        new BigDecimal("490"),
                        new BigDecimal("2.142")
                )),
                List.of(new NewsArticle(
                        "Markets open higher",
                        "https://example.com/story",
                        "Wire",
                        "2026-10-29T13:00:00Z",
                        "Summary",
                        null,
                        null
                ))
        );

        assertTrue(line.contains("SPY  500.50  +2.14%"));
        assertTrue(line.contains("Wire  Markets open higher"));
        assertFalse(line.contains("490"));
    }

    @Test
    void saysWhenNothingWasReturned() {
        assertTrue(TapeLine.format(List.of(), List.of()).contains("UNAVAILABLE"));
    }
}
