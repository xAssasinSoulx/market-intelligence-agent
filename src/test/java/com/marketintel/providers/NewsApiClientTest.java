package com.marketintel.providers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.marketintel.model.NewsArticle;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NewsApiClientTest {

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    @Test
    void parsesTickerSpecificSentiment()
            throws Exception {

        String json =
                """
                {
                  "feed": [
                    {
                      "title": "NVIDIA launches new product",
                      "url": "https://example.com/article1",
                      "source": "Example News",
                      "time_published": "20261001T203000",
                      "summary": "NVIDIA announced a new product.",
                      "overall_sentiment_score": "0.10",
                      "overall_sentiment_label": "Neutral",
                      "ticker_sentiment": [
                        {
                          "ticker": "NVDA",
                          "ticker_sentiment_score": "0.35",
                          "ticker_sentiment_label": "Bullish"
                        }
                      ]
                    }
                  ]
                }
                """;

        JsonNode root =
                objectMapper.readTree(
                        json
                );

        NewsApiClient client =
                new NewsApiClient(
                        new AlphaVantageClient(
                                "dummy-key"
                        )
                );

        List<NewsArticle> articles =
                client.parseNews(
                        "NVDA",
                        root,
                        5
                );

        assertEquals(
                1,
                articles.size()
        );

        NewsArticle article =
                articles.getFirst();

        assertEquals(
                "NVIDIA launches new product",
                article.getTitle()
        );

        assertEquals(
                "Example News",
                article.getSource()
        );

        assertEquals(
                new BigDecimal(
                        "0.35"
                ),
                article.getSentimentScore()
        );

        assertEquals(
                "Bullish",
                article.getSentimentLabel()
        );
    }

    @Test
    void fallsBackToOverallSentiment()
            throws Exception {

        String json =
                """
                {
                  "feed": [
                    {
                      "title": "Technology market update",
                      "url": "https://example.com/article2",
                      "source": "Example News",
                      "time_published": "20261001T190000",
                      "summary": "A broad market update.",
                      "overall_sentiment_score": "-0.20",
                      "overall_sentiment_label": "Somewhat-Bearish",
                      "ticker_sentiment": []
                    }
                  ]
                }
                """;

        JsonNode root =
                objectMapper.readTree(
                        json
                );

        NewsApiClient client =
                new NewsApiClient(
                        new AlphaVantageClient(
                                "dummy-key"
                        )
                );

        List<NewsArticle> articles =
                client.parseNews(
                        "NVDA",
                        root,
                        5
                );

        assertEquals(
                1,
                articles.size()
        );

        assertEquals(
                new BigDecimal(
                        "-0.20"
                ),
                articles
                        .getFirst()
                        .getSentimentScore()
        );
    }

    @Test
    void skipsArticlesWithoutTitleOrUrl()
            throws Exception {

        String json =
                """
                {
                  "feed": [
                    {
                      "title": "Valid article",
                      "url": "https://example.com/valid"
                    },
                    {
                      "title": "Missing URL"
                    },
                    {
                      "url": "https://example.com/missing-title"
                    }
                  ]
                }
                """;

        JsonNode root =
                objectMapper.readTree(
                        json
                );

        NewsApiClient client =
                new NewsApiClient(
                        new AlphaVantageClient(
                                "dummy-key"
                        )
                );

        List<NewsArticle> articles =
                client.parseNews(
                        "NVDA",
                        root,
                        5
                );

        assertEquals(
                1,
                articles.size()
        );
    }

    @Test
    void respectsRequestedLimit()
            throws Exception {

        String json =
                """
                {
                  "feed": [
                    {
                      "title": "Article 1",
                      "url": "https://example.com/1"
                    },
                    {
                      "title": "Article 2",
                      "url": "https://example.com/2"
                    },
                    {
                      "title": "Article 3",
                      "url": "https://example.com/3"
                    }
                  ]
                }
                """;

        JsonNode root =
                objectMapper.readTree(
                        json
                );

        NewsApiClient client =
                new NewsApiClient(
                        new AlphaVantageClient(
                                "dummy-key"
                        )
                );

        List<NewsArticle> articles =
                client.parseNews(
                        "NVDA",
                        root,
                        2
                );

        assertEquals(
                2,
                articles.size()
        );
    }
}