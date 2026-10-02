package com.marketintel.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class NewsResult {

    private final String symbol;

    private final List<NewsArticle> articles;

    public NewsResult(
            String symbol,
            List<NewsArticle> articles) {

        this.symbol =
                Objects.requireNonNull(
                        symbol,
                        "Symbol cannot be null."
                );

        this.articles =
                new ArrayList<>(
                        Objects.requireNonNull(
                                articles,
                                "Articles cannot be null."
                        )
                );
    }

    public String getSymbol() {
        return symbol;
    }

    public List<NewsArticle> getArticles() {

        return Collections.unmodifiableList(
                articles
        );
    }
}