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

    private NewsApiClient createClient() {

        return new NewsApiClient(
                new AlphaVantageClient(
                        "dummy-key"
                )
        );
    }

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

        List<NewsArticle> articles =
                createClient()
                        .parseNews(
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
    void filtersArticlesForOtherTickers()
            throws Exception {

        String json =
                """
                {
                  "feed": [
                    {
                      "title": "Coca-Cola vs PepsiCo",
                      "url": "https://example.com/coke",
                      "ticker_sentiment": [
                        {
                          "ticker": "KO",
                          "ticker_sentiment_score": "0.33",
                          "ticker_sentiment_label": "Bullish"
                        },
                        {
                          "ticker": "PEP",
                          "ticker_sentiment_score": "0.20",
                          "ticker_sentiment_label": "Bullish"
                        }
                      ]
                    },
                    {
                      "title": "NVIDIA launches new GPU",
                      "url": "https://example.com/nvidia",
                      "ticker_sentiment": [
                        {
                          "ticker": "NVDA",
                          "ticker_sentiment_score": "0.41",
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

        List<NewsArticle> articles =
                createClient()
                        .parseNews(
                                "NVDA",
                                root,
                                5
                        );

        assertEquals(
                1,
                articles.size()
        );

        assertEquals(
                "NVIDIA launches new GPU",
                articles
                        .getFirst()
                        .getTitle()
        );
    }

    @Test
    void rejectsArticleWithoutRequestedTicker()
            throws Exception {

        String json =
                """
                {
                  "feed": [
                    {
                      "title": "General technology news",
                      "url": "https://example.com/general",
                      "overall_sentiment_score": "0.50",
                      "overall_sentiment_label": "Bullish",
                      "ticker_sentiment": [
                        {
                          "ticker": "MSFT",
                          "ticker_sentiment_score": "0.50",
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

        List<NewsArticle> articles =
                createClient()
                        .parseNews(
                                "NVDA",
                                root,
                                5
                        );

        assertTrue(
                articles.isEmpty()
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
                      "title": "Valid NVIDIA article",
                      "url": "https://example.com/valid",
                      "ticker_sentiment": [
                        {
                          "ticker": "NVDA",
                          "ticker_sentiment_score": "0.25",
                          "ticker_sentiment_label": "Bullish"
                        }
                      ]
                    },
                    {
                      "title": "Missing URL",
                      "ticker_sentiment": [
                        {
                          "ticker": "NVDA"
                        }
                      ]
                    },
                    {
                      "url": "https://example.com/missing-title",
                      "ticker_sentiment": [
                        {
                          "ticker": "NVDA"
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

        List<NewsArticle> articles =
                createClient()
                        .parseNews(
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
    void respectsRequestedLimitAfterFiltering()
            throws Exception {

        String json =
                """
                {
                  "feed": [
                    {
                      "title": "Unrelated article",
                      "url": "https://example.com/unrelated",
                      "ticker_sentiment": [
                        {
                          "ticker": "MSFT"
                        }
                      ]
                    },
                    {
                      "title": "NVIDIA Article 1",
                      "url": "https://example.com/1",
                      "ticker_sentiment": [
                        {
                          "ticker": "NVDA"
                        }
                      ]
                    },
                    {
                      "title": "NVIDIA Article 2",
                      "url": "https://example.com/2",
                      "ticker_sentiment": [
                        {
                          "ticker": "NVDA"
                        }
                      ]
                    },
                    {
                      "title": "NVIDIA Article 3",
                      "url": "https://example.com/3",
                      "ticker_sentiment": [
                        {
                          "ticker": "NVDA"
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

        List<NewsArticle> articles =
                createClient()
                        .parseNews(
                                "NVDA",
                                root,
                                2
                        );

        assertEquals(
                2,
                articles.size()
        );

        assertEquals(
                "NVIDIA Article 1",
                articles.get(0)
                        .getTitle()
        );

        assertEquals(
                "NVIDIA Article 2",
                articles.get(1)
                        .getTitle()
        );
    }
}