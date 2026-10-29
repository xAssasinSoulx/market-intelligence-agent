package com.marketintel.providers;

import com.marketintel.model.MarketQuote;
import com.marketintel.model.NewsArticle;
import com.marketintel.model.SecurityOverview;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyProviderAdapterTest {

    @Test
    void alphaVantageAdapterDelegatesWithoutClaimingStatements() {
        AtomicReference<String> quoted = new AtomicReference<>();
        MarketDataProvider quotes = new MarketDataProvider() {
            @Override
            public MarketQuote getQuote(String symbol) {
                quoted.set(symbol);
                return new MarketQuote(symbol, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO);
            }

            @Override
            public SecurityOverview getSecurityOverview(String symbol) {
                return new SecurityOverview(symbol, "Name", "", "", "USD", "", "", null);
            }
        };
        ETFDataProvider etf = symbol -> {
            throw new MarketDataException("etf " + symbol);
        };
        NewsProvider news = (symbol, limit) -> List.of(
                new NewsArticle("Title", "https://example.com", null, null, null, null, null)
        );

        AlphaVantageAdapter adapter = new AlphaVantageAdapter(quotes, etf, news);

        assertTrue(adapter.supports(DataCapability.QUOTE));
        assertTrue(adapter.supports(DataCapability.NEWS));
        assertFalse(adapter.supports(DataCapability.PRICE_HISTORY));
        assertEquals("NVDA", adapter.getQuote("NVDA").getSymbol());
        assertEquals("NVDA", quoted.get());
        assertEquals("Title", adapter.getNews("NVDA", 1).getFirst().getTitle());
    }

    @Test
    void marketauxAdapterServesNewsOnly() {
        List<NewsArticle> articles = List.of(
                new NewsArticle("Headline", "https://example.com/a", null, null, null, null, null)
        );
        MarketauxAdapter adapter = new MarketauxAdapter((symbol, limit) -> articles);

        assertTrue(adapter.supports(DataCapability.NEWS));
        assertFalse(adapter.supports(DataCapability.QUOTE));
        assertSame(articles, adapter.getNews("SPY", 3));
    }
}
