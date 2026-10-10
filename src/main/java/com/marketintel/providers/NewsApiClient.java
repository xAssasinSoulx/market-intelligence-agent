package com.marketintel.providers;

import com.fasterxml.jackson.databind.JsonNode;

import com.marketintel.model.NewsArticle;

import java.math.BigDecimal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class NewsApiClient
        implements NewsProvider {

    private static final int MAX_LIMIT =
            100;

    private static final BigDecimal
            MIN_RELEVANCE_SCORE =
            new BigDecimal("0.20");

    private final AlphaVantageClient client;

    public NewsApiClient(
            AlphaVantageClient client) {

        if (client == null) {

            throw new IllegalArgumentException(
                    "Alpha Vantage client "
                            + "cannot be null."
            );
        }

        this.client =
                client;
    }

    @Override
    public List<NewsArticle> getNews(
            String symbol,
            int limit) {

        String normalizedSymbol =
                normalizeSymbol(
                        symbol
                );

        int normalizedLimit =
                normalizeLimit(
                        limit
                );

        /*
         * Retrieve a larger recent candidate pool.
         *
         * We independently rank the results using
         * ticker-specific relevance before returning
         * them to the user.
         */
        int providerLimit =
                Math.min(
                        MAX_LIMIT,
                        Math.max(
                                normalizedLimit * 20,
                                normalizedLimit
                        )
                );

        JsonNode root =
                client.query(
                        "NEWS_SENTIMENT",

                        Map.of(
                                "tickers",
                                normalizedSymbol,

                                "sort",
                                "LATEST",

                                "limit",
                                String.valueOf(
                                        providerLimit
                                )
                        )
                );

        return parseNews(
                normalizedSymbol,
                root,
                normalizedLimit
        );
    }

    List<NewsArticle> parseNews(
            String symbol,
            JsonNode root,
            int limit) {

        if (root == null
                || root.isNull()) {

            throw new MarketDataException(
                    "News response was empty."
            );
        }

        JsonNode feed =
                root.path(
                        "feed"
                );

        if (!feed.isArray()) {

            return List.of();
        }

        List<NewsCandidate> candidates =
                new ArrayList<>();

        /*
         * IMPORTANT:
         *
         * Do not stop once we have five articles.
         *
         * The provider response is sorted by recency,
         * not by ticker relevance. We therefore inspect
         * the whole candidate pool first and rank it
         * afterward.
         */
        for (JsonNode articleNode
                : feed) {

            String title =
                    textOrNull(
                            articleNode,
                            "title"
                    );

            String url =
                    textOrNull(
                            articleNode,
                            "url"
                    );

            if (title == null
                    || url == null) {

                continue;
            }

            SentimentData sentiment =
                    extractSentiment(
                            symbol,
                            articleNode
                    );

            /*
             * No ticker-specific information means the
             * article is not sufficiently grounded in
             * the requested security.
             */
            if (sentiment.relevanceScore()
                    == null) {

                continue;
            }

            /*
             * Ignore passing references to the ticker.
             */
            if (sentiment
                    .relevanceScore()
                    .compareTo(
                            MIN_RELEVANCE_SCORE
                    ) < 0) {

                continue;
            }

            String source =
                    textOrNull(
                            articleNode,
                            "source"
                    );

            String publishedAt =
                    textOrNull(
                            articleNode,
                            "time_published"
                    );

            String summary =
                    textOrNull(
                            articleNode,
                            "summary"
                    );

            NewsArticle article =
                    new NewsArticle(
                            title,
                            url,
                            source,
                            publishedAt,
                            summary,
                            sentiment.score(),
                            sentiment.label()
                    );

            candidates.add(
                    new NewsCandidate(
                            article,
                            sentiment.relevanceScore()
                    )
            );
        }

        /*
         * Rank by ticker relevance.
         *
         * Java's list sort is stable, so articles with
         * equal relevance preserve the provider's
         * original recency ordering.
         */
        candidates.sort(
                Comparator
                        .comparing(
                                NewsCandidate::relevanceScore
                        )
                        .reversed()
        );

        return candidates
                .stream()
                .limit(
                        limit
                )
                .map(
                        NewsCandidate::article
                )
                .toList();
    }

    private SentimentData extractSentiment(
            String symbol,
            JsonNode articleNode) {

        JsonNode tickerSentiment =
                articleNode.path(
                        "ticker_sentiment"
                );

        if (!tickerSentiment.isArray()) {

            return new SentimentData(
                    null,
                    null,
                    null
            );
        }

        for (JsonNode tickerNode
                : tickerSentiment) {

            String ticker =
                    textOrNull(
                            tickerNode,
                            "ticker"
                    );

            if (ticker == null
                    || !ticker.equalsIgnoreCase(
                    symbol
            )) {

                continue;
            }

            return new SentimentData(
                    decimalOrNull(
                            tickerNode,
                            "ticker_sentiment_score"
                    ),

                    textOrNull(
                            tickerNode,
                            "ticker_sentiment_label"
                    ),

                    decimalOrNull(
                            tickerNode,
                            "relevance_score"
                    )
            );
        }

        return new SentimentData(
                null,
                null,
                null
        );
    }

    private String textOrNull(
            JsonNode node,
            String field) {

        JsonNode value =
                node.path(
                        field
                );

        if (value.isMissingNode()
                || value.isNull()) {

            return null;
        }

        String text =
                value.asText()
                        .trim();

        if (text.isBlank()
                || text.equalsIgnoreCase(
                "None"
        )) {

            return null;
        }

        return text;
    }

    private BigDecimal decimalOrNull(
            JsonNode node,
            String field) {

        String value =
                textOrNull(
                        node,
                        field
                );

        if (value == null) {

            return null;
        }

        try {

            return new BigDecimal(
                    value
            );

        } catch (
                NumberFormatException e) {

            return null;
        }
    }

    private int normalizeLimit(
            int limit) {

        if (limit <= 0) {

            throw new IllegalArgumentException(
                    "News limit must be positive."
            );
        }

        return Math.min(
                limit,
                MAX_LIMIT
        );
    }

    private String normalizeSymbol(
            String symbol) {

        if (symbol == null
                || symbol.isBlank()) {

            throw new IllegalArgumentException(
                    "Symbol cannot be blank."
            );
        }

        return symbol
                .trim()
                .toUpperCase();
    }

    private record SentimentData(
            BigDecimal score,
            String label,
            BigDecimal relevanceScore) {
    }

    private record NewsCandidate(
            NewsArticle article,
            BigDecimal relevanceScore) {
    }
}