package com.marketintel.providers;

import com.marketintel.model.NewsArticle;

import java.util.List;

/**
 * Adapter that presents {@link MarketauxNewsClient} through
 * {@link DataProvider}. Marketaux remains the first news route when its
 * token is set, because its articles carry a per-ticker sentiment score.
 */
public final class MarketauxAdapter implements DataProvider {

    private final NewsProvider news;

    public MarketauxAdapter(NewsProvider news) {
        if (news == null) {
            throw new IllegalArgumentException("Marketaux client cannot be null.");
        }
        this.news = news;
    }

    @Override
    public String providerName() {
        return "Marketaux";
    }

    @Override
    public boolean supports(DataCapability capability) {
        return capability == DataCapability.NEWS;
    }

    @Override
    public List<NewsArticle> getNews(String symbol, int limit) {
        return news.getNews(symbol, limit);
    }
}
