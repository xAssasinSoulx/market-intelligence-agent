package com.marketintel.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketintel.model.NewsArticle;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinnhubAdapterTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final FinnhubAdapter adapter =
            new FinnhubAdapter(new FinnhubMarketDataClient("dummy-key"));

    @Test
    void servesQuotesOverviewsAndNewsOnly() {
        assertTrue(adapter.supports(DataCapability.QUOTE));
        assertTrue(adapter.supports(DataCapability.OVERVIEW));
        assertTrue(adapter.supports(DataCapability.NEWS));
        assertFalse(adapter.supports(DataCapability.FINANCIAL_STATEMENTS));
        assertEquals("USD", adapter.quoteCurrency().orElseThrow());

        MarketDataException error = assertThrows(
                MarketDataException.class,
                () -> adapter.getFinancialStatements("AAPL")
        );
        assertTrue(error.getMessage().contains("FINANCIAL_STATEMENTS"));
    }

    @Test
    void parsesCompanyNewsAndSkipsAnUnrelatedTicker() throws Exception {
        var root = mapper.readTree("""
                [
                  {
                    "headline": "NVIDIA raises guidance",
                    "url": "https://example.com/nvda",
                    "source": "Example Wire",
                    "summary": "Guidance was raised.",
                    "datetime": 1761681600,
                    "related": "NVDA"
                  },
                  {
                    "headline": "Other story",
                    "url": "https://example.com/other",
                    "related": "AAPL"
                  },
                  {
                    "headline": "",
                    "url": "https://example.com/blank"
                  }
                ]
                """);

        List<NewsArticle> articles = adapter.parseCompanyNews("NVDA", root, 10);

        assertEquals(1, articles.size());
        assertEquals("NVIDIA raises guidance", articles.getFirst().getTitle());
        assertEquals("https://example.com/nvda", articles.getFirst().getUrl());
        assertEquals("Example Wire", articles.getFirst().getSource());
        assertEquals("2025-10-28T20:00:00Z", articles.getFirst().getPublishedAt());
        assertNull(articles.getFirst().getSentimentScore());
    }

    @Test
    void keepsAnArticleWhenFinnhubOmitsTheRelatedField() throws Exception {
        var root = mapper.readTree("""
                [{"headline": "Desk note", "url": "https://example.com/note", "datetime": 0}]
                """);

        List<NewsArticle> articles = adapter.parseCompanyNews("NVDA", root, 3);

        assertEquals(1, articles.size());
        assertNull(articles.getFirst().getPublishedAt());
    }
}
