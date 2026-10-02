package com.marketintel.providers;

import com.fasterxml.jackson.databind.JsonNode;

import com.marketintel.model.NewsArticle;

import java.math.BigDecimal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NewsApiClient
        implements NewsProvider {

    private static final int MAX_LIMIT =
            50;

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
         * Retrieve a larger candidate pool because we
         * independently verify ticker relevance before
         * exposing articles to the user.
         */
        int providerLimit =
                Math.min(
                        MAX_LIMIT,
                        Math.max(
                                normalizedLimit * 10,
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

        List<NewsArticle> articles =
                new ArrayList<>();

        for (JsonNode articleNode
                : feed) {

            if (articles.size()
                    >= limit) {

                break;
            }

            /*
             * Do not blindly trust the provider-side
             * ticker filter. The article must explicitly
             * contain a ticker_sentiment entry for the
             * requested symbol.
             */
            if (!mentionsTicker(
                    symbol,
                    articleNode
            )) {

                continue;
            }

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

            SentimentData sentiment =
                    extractSentiment(
                            symbol,
                            articleNode
                    );

            articles.add(
                    new NewsArticle(
                            title,
                            url,
                            source,
                            publishedAt,
                            summary,
                            sentiment.score(),
                            sentiment.label()
                    )
            );
        }

        return List.copyOf(
                articles
        );
    }

    private boolean mentionsTicker(
            String symbol,
            JsonNode articleNode) {

        JsonNode tickerSentiment =
                articleNode.path(
                        "ticker_sentiment"
                );

        if (!tickerSentiment.isArray()) {

            return false;
        }

        for (JsonNode tickerNode
                : tickerSentiment) {

            String ticker =
                    textOrNull(
                            tickerNode,
                            "ticker"
                    );

            if (ticker != null
                    && ticker.equalsIgnoreCase(
                    symbol
            )) {

                return true;
            }
        }

        return false;
    }

    private SentimentData extractSentiment(
            String symbol,
            JsonNode articleNode) {

        JsonNode tickerSentiment =
                articleNode.path(
                        "ticker_sentiment"
                );

        if (tickerSentiment.isArray()) {

            for (JsonNode tickerNode
                    : tickerSentiment) {

                String ticker =
                        textOrNull(
                                tickerNode,
                                "ticker"
                        );

                if (ticker != null
                        && ticker.equalsIgnoreCase(
                        symbol
                )) {

                    return new SentimentData(
                            decimalOrNull(
                                    tickerNode,
                                    "ticker_sentiment_score"
                            ),

                            textOrNull(
                                    tickerNode,
                                    "ticker_sentiment_label"
                            )
                    );
                }
            }
        }

        return new SentimentData(
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
            String label) {
    }
}