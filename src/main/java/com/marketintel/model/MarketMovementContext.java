
package com.marketintel.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class MarketMovementContext {

    private final MarketQuote quote;
    private final List<NewsArticle> articles;
    private final Instant retrievedAt;
    private final String newsWarning;

    public MarketMovementContext(
            MarketQuote quote,
            List<NewsArticle> articles,
            Instant retrievedAt,
            String newsWarning) {

        this.quote = Objects.requireNonNull(
                quote,
                "Market quote cannot be null."
        );

        this.articles = List.copyOf(
                Objects.requireNonNull(
                        articles,
                        "News articles cannot be null."
                )
        );

        this.retrievedAt = Objects.requireNonNull(
                retrievedAt,
                "Retrieval time cannot be null."
        );

        this.newsWarning = newsWarning;
    }

    public String getSymbol() {
        return quote.getSymbol();
    }

    public MarketQuote getQuote() {
        return quote;
    }

    public List<NewsArticle> getArticles() {
        return articles;
    }

    public Instant getRetrievedAt() {
        return retrievedAt;
    }

    public String getNewsWarning() {
        return newsWarning;
    }
}
