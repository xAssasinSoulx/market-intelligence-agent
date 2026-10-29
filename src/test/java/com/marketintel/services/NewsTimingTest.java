package com.marketintel.services;

import com.marketintel.model.MarketQuote;
import com.marketintel.model.NewsArticle;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NewsTimingTest {

    private static final Instant LAST_TRADE = Instant.parse("2026-10-28T20:00:00Z");

    @Test
    void parsesIsoAndCompactTimestamps() {
        assertEquals(
                Instant.parse("2026-10-28T19:55:24Z"),
                NewsTiming.parsePublishedAt("2026-10-28T19:55:24.000000Z").orElseThrow()
        );
        assertEquals(
                Instant.parse("2026-10-18T14:25:00Z"),
                NewsTiming.parsePublishedAt("20261018T142500").orElseThrow()
        );
        assertTrue(NewsTiming.parsePublishedAt("yesterday").isEmpty());
        assertTrue(NewsTiming.parsePublishedAt(null).isEmpty());
    }

    @Test
    void flagsOnlyArticlesPublishedAfterAKnownLastTrade() {
        MarketQuote timed = quote(LAST_TRADE);

        assertTrue(NewsTiming.publishedAfterLastTrade(
                article("2026-10-28T21:30:00Z"), timed
        ));
        assertFalse(NewsTiming.publishedAfterLastTrade(
                article("2026-10-28T18:37:53Z"), timed
        ));
        assertFalse(NewsTiming.publishedAfterLastTrade(
                article("not a time"), timed
        ));
    }

    @Test
    void neverFlagsWhenTheQuoteHasNoLastTradeTime() {
        assertFalse(NewsTiming.publishedAfterLastTrade(
                article("2026-10-28T21:30:00Z"), quote(null)
        ));
    }

    @Test
    void formatsUtcForDisplay() {
        assertEquals("2026-10-28 20:00 UTC", NewsTiming.formatUtc(LAST_TRADE));
    }

    private static MarketQuote quote(Instant lastTrade) {
        return new MarketQuote(
                "AAPL",
                new BigDecimal("343.83"),
                new BigDecimal("347.10"),
                new BigDecimal("-0.9421"),
                lastTrade
        );
    }

    private static NewsArticle article(String publishedAt) {
        return new NewsArticle(
                "Headline",
                "https://example.com/story",
                "Wire",
                publishedAt,
                null,
                null,
                null
        );
    }
}
