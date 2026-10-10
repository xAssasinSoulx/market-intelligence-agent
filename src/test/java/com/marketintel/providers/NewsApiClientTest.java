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
                          "relevance_score": "0.85",
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
                          "relevance_score": "0.90",
                          "ticker_sentiment_score": "0.33",
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
                          "relevance_score": "0.80",
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
                          "relevance_score": "0.75",
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
                          "relevance_score": "0.75",
                          "ticker_sentiment_score": "0.25",
                          "ticker_sentiment_label": "Bullish"
                        }
                      ]
                    },
                    {
                      "title": "Missing URL",
                      "ticker_sentiment": [
                        {
                          "ticker": "NVDA",
                          "relevance_score": "0.80"
                        }
                      ]
                    },
                    {
                      "url": "https://example.com/missing-title",
                      "ticker_sentiment": [
                        {
                          "ticker": "NVDA",
                          "relevance_score": "0.80"
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
                "Valid NVIDIA article",
                articles
                        .getFirst()
                        .getTitle()
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
                          "ticker": "MSFT",
                          "relevance_score": "0.90"
                        }
                      ]
                    },
                    {
                      "title": "NVIDIA Article 1",
                      "url": "https://example.com/1",
                      "ticker_sentiment": [
                        {
                          "ticker": "NVDA",
                          "relevance_score": "0.80"
                        }
                      ]
                    },
                    {
                      "title": "NVIDIA Article 2",
                      "url": "https://example.com/2",
                      "ticker_sentiment": [
                        {
                          "ticker": "NVDA",
                          "relevance_score": "0.70"
                        }
                      ]
                    },
                    {
                      "title": "NVIDIA Article 3",
                      "url": "https://example.com/3",
                      "ticker_sentiment": [
                        {
                          "ticker": "NVDA",
                          "relevance_score": "0.60"
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
                articles
                        .get(0)
                        .getTitle()
        );

        assertEquals(
                "NVIDIA Article 2",
                articles
                        .get(1)
                        .getTitle()
        );
    }

    @Test
    void filtersLowRelevanceTickerMentions()
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
                          "relevance_score": "0.75",
                          "ticker_sentiment_score": "0.30",
                          "ticker_sentiment_label": "Bullish"
                        },
                        {
                          "ticker": "NVDA",
                          "relevance_score": "0.02",
                          "ticker_sentiment_score": "0.33",
                          "ticker_sentiment_label": "Somewhat-Bullish"
                        }
                      ]
                    },
                    {
                      "title": "NVIDIA launches new AI platform",
                      "url": "https://example.com/nvidia",
                      "ticker_sentiment": [
                        {
                          "ticker": "NVDA",
                          "relevance_score": "0.82",
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
                "NVIDIA launches new AI platform",
                articles
                        .getFirst()
                        .getTitle()
        );
    }

    @Test
    void returnsMostRelevantArticlesInsteadOfFirstMatching()
            throws Exception {

        String json =
                """
                {
                  "feed": [
                    {
                      "title": "Newest but weak NVIDIA mention",
                      "url": "https://example.com/weak",
                      "ticker_sentiment": [
                        {
                          "ticker": "NVDA",
                          "relevance_score": "0.25",
                          "ticker_sentiment_score": "0.10",
                          "ticker_sentiment_label": "Neutral"
                        }
                      ]
                    },
                    {
                      "title": "Strong NVIDIA story",
                      "url": "https://example.com/strong",
                      "ticker_sentiment": [
                        {
                          "ticker": "NVDA",
                          "relevance_score": "0.92",
                          "ticker_sentiment_score": "0.45",
                          "ticker_sentiment_label": "Bullish"
                        }
                      ]
                    },
                    {
                      "title": "Another relevant NVIDIA story",
                      "url": "https://example.com/relevant",
                      "ticker_sentiment": [
                        {
                          "ticker": "NVDA",
                          "relevance_score": "0.75",
                          "ticker_sentiment_score": "0.30",
                          "ticker_sentiment_label": "Somewhat-Bullish"
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
                "Strong NVIDIA story",
                articles.get(0)
                        .getTitle()
        );

        assertEquals(
                "Another relevant NVIDIA story",
                articles.get(1)
                        .getTitle()
        );
    }
}