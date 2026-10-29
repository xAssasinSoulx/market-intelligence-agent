package com.marketintel.providers;

import com.marketintel.model.ETFProfile;
import com.marketintel.model.MarketQuote;
import com.marketintel.model.NewsArticle;
import com.marketintel.model.SecurityOverview;

import java.util.List;

/**
 * Adapter that presents the existing Alpha Vantage clients as one
 * {@link DataProvider}.
 *
 * Capability and routing are separate. This adapter can serve quotes,
 * overviews, ETF profiles, and news. Startup does not register the news
 * capability, because the free plan is about 25 requests a day and news
 * is already covered by Marketaux or Finnhub.
 */
public final class AlphaVantageAdapter implements DataProvider {

    private final MarketDataProvider quotes;
    private final ETFDataProvider etfProfiles;
    private final NewsProvider news;

    public AlphaVantageAdapter(
            MarketDataProvider quotes,
            ETFDataProvider etfProfiles,
            NewsProvider news) {

        if (quotes == null) {
            throw new IllegalArgumentException("Alpha Vantage quote client cannot be null.");
        }
        if (etfProfiles == null) {
            throw new IllegalArgumentException("Alpha Vantage ETF client cannot be null.");
        }
        if (news == null) {
            throw new IllegalArgumentException("Alpha Vantage news client cannot be null.");
        }
        this.quotes = quotes;
        this.etfProfiles = etfProfiles;
        this.news = news;
    }

    @Override
    public String providerName() {
        return "Alpha Vantage";
    }

    @Override
    public boolean supports(DataCapability capability) {
        return capability == DataCapability.QUOTE
                || capability == DataCapability.OVERVIEW
                || capability == DataCapability.ETF_PROFILE
                || capability == DataCapability.NEWS;
    }

    @Override
    public java.util.Optional<String> quoteCurrency() {
        return quotes.quoteCurrency();
    }

    @Override
    public MarketQuote getQuote(String symbol) {
        return quotes.getQuote(symbol);
    }

    @Override
    public SecurityOverview getSecurityOverview(String symbol) {
        return quotes.getSecurityOverview(symbol);
    }

    @Override
    public ETFProfile getEtfProfile(String symbol) {
        return etfProfiles.getProfile(symbol);
    }

    @Override
    public List<NewsArticle> getNews(String symbol, int limit) {
        return news.getNews(symbol, limit);
    }
}
