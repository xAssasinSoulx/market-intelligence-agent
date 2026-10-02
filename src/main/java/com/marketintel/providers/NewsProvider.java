package com.marketintel.providers;

import com.marketintel.model.NewsArticle;

import java.util.List;

public interface NewsProvider {

    List<NewsArticle> getNews(
            String symbol,
            int limit
    );
}