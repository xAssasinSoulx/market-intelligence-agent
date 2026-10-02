package com.marketintel.model;

import java.math.BigDecimal;
import java.util.Objects;

public class NewsArticle {

    private final String title;

    private final String url;

    private final String source;

    private final String publishedAt;

    private final String summary;

    private final BigDecimal sentimentScore;

    private final String sentimentLabel;

    public NewsArticle(
            String title,
            String url,
            String source,
            String publishedAt,
            String summary,
            BigDecimal sentimentScore,
            String sentimentLabel) {

        this.title =
                Objects.requireNonNull(
                        title,
                        "Title cannot be null."
                );

        this.url =
                Objects.requireNonNull(
                        url,
                        "URL cannot be null."
                );

        this.source =
                source;

        this.publishedAt =
                publishedAt;

        this.summary =
                summary;

        this.sentimentScore =
                sentimentScore;

        this.sentimentLabel =
                sentimentLabel;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public String getSource() {
        return source;
    }

    public String getPublishedAt() {
        return publishedAt;
    }

    public String getSummary() {
        return summary;
    }

    public BigDecimal getSentimentScore() {
        return sentimentScore;
    }

    public String getSentimentLabel() {
        return sentimentLabel;
    }
}