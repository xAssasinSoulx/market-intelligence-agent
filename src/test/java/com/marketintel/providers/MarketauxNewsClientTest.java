
package com.marketintel.providers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketintel.model.NewsArticle;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.time.Instant;
import java.time.temporal.ChronoUnit;


import static org.junit.jupiter.api.Assertions.*;

class MarketauxNewsClientTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private final MarketauxNewsClient client =
            new MarketauxNewsClient("dummy-token");

    @Test
    void parsesRelevantNewsAndSentiment() throws Exception {

        String json = """
            {
              "data": [
                {
                  "title": "NVIDIA announces new AI platform",
                  "url": "https://example.com/nvda",
                  "source": "example.com",
                  "published_at": "2026-10-18T14:25:00.000000Z",
                  "description": "New AI infrastructure.",
                  "entities": [
                    {
                      "symbol": "NVDA",
                      "match_score": 82.5,
                      "sentiment_score": 0.42
                    }
                  ]
                }
              ]
            }
            """;

        JsonNode root = mapper.readTree(json);

        List<NewsArticle> articles =
                client.parseNews("NVDA", root, 3);

        assertEquals(1, articles.size());

        NewsArticle article = articles.getFirst();

        assertEquals(
                "NVIDIA announces new AI platform",
                article.getTitle()
        );

        assertEquals(
                new BigDecimal("0.42"),
                article.getSentimentScore()
        );

        assertEquals(
                "Bullish",
                article.getSentimentLabel()
        );
    }

    @Test
    void rejectsUnrelatedArticles() throws Exception {

        String json = """
            {
              "data": [
                {
                  "title": "Coca-Cola news",
                  "url": "https://example.com/ko",
                  "entities": [
                    {
                      "symbol": "KO",
                      "sentiment_score": 0.6
                    }
                  ]
                }
              ]
            }
            """;

        JsonNode root = mapper.readTree(json);

        List<NewsArticle> articles =
                client.parseNews("NVDA", root, 3);

        assertTrue(articles.isEmpty());
    }

    @Test
    void respectsArticleLimit() throws Exception {

        String json = """
            {
              "data": [
                {
                  "title": "Article One",
                  "url": "https://example.com/1",
                  "entities": [{"symbol": "NVDA"}]
                },
                {
                  "title": "Article Two",
                  "url": "https://example.com/2",
                  "entities": [{"symbol": "NVDA"}]
                },
                {
                  "title": "Article Three",
                  "url": "https://example.com/3",
                  "entities": [{"symbol": "NVDA"}]
                }
              ]
            }
            """;

        JsonNode root = mapper.readTree(json);

        List<NewsArticle> articles =
                client.parseNews("NVDA", root, 2);

        assertEquals(2, articles.size());
    }

    @Test
    void filtersOutdatedNews() {

        Instant now =
                Instant.parse("2026-10-18T12:00:00Z");

        Instant cutoff =
                now.minus(7, ChronoUnit.DAYS);

        assertTrue(
                client.isRecent(
                        "2026-10-17T15:30:00Z",
                        cutoff,
                        now
                )
        );

        assertFalse(
                client.isRecent(
                        "2024-05-22T14:10:00Z",
                        cutoff,
                        now
                )
        );

        assertFalse(
                client.isRecent(
                        "2026-03-25T16:25:00Z",
                        cutoff,
                        now
                )
        );

        assertFalse(
                client.isRecent(
                        null,
                        cutoff,
                        now
                )
        );
    }
}

