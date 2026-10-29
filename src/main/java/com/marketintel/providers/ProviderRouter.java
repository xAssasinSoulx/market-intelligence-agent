package com.marketintel.providers;

import com.marketintel.model.ETFProfile;
import com.marketintel.model.FinancialStatements;
import com.marketintel.model.MarketQuote;
import com.marketintel.model.NewsArticle;
import com.marketintel.model.PriceHistory;
import com.marketintel.model.SecurityOverview;
import com.marketintel.model.SentimentReading;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Composite in front of the adapters. The first adapter registered for a
 * capability handles that capability. A later adapter is the fallback
 * used only when an earlier one was not registered, not when a call fails.
 */
public final class ProviderRouter implements MarketDataProvider, NewsProvider, ETFDataProvider,
        FinancialStatementProvider, PriceHistoryProvider, SentimentProvider {

    private final Map<DataCapability, List<DataProvider>> routes = new EnumMap<>(DataCapability.class);

    public void prefer(DataCapability capability, DataProvider provider) {
        if (capability == null) {
            throw new IllegalArgumentException("Capability cannot be null.");
        }
        if (provider == null) {
            throw new IllegalArgumentException("Data provider cannot be null.");
        }
        if (!provider.supports(capability)) {
            throw new IllegalArgumentException(
                    provider.providerName() + " does not serve " + capability + "."
            );
        }
        List<DataProvider> ordered = routes.computeIfAbsent(capability, key -> new ArrayList<>());
        if (!ordered.contains(provider)) {
            ordered.add(provider);
        }
    }

    public boolean serves(DataCapability capability) {
        List<DataProvider> ordered = routes.get(capability);
        return ordered != null && !ordered.isEmpty();
    }

    @Override
    public MarketQuote getQuote(String symbol) {
        return first(DataCapability.QUOTE).getQuote(symbol);
    }

    @Override
    public SecurityOverview getSecurityOverview(String symbol) {
        return first(DataCapability.OVERVIEW).getSecurityOverview(symbol);
    }

    @Override
    public Optional<String> quoteCurrency() {
        if (!serves(DataCapability.QUOTE)) {
            return Optional.empty();
        }
        return first(DataCapability.QUOTE).quoteCurrency();
    }

    @Override
    public List<NewsArticle> getNews(String symbol, int limit) {
        return first(DataCapability.NEWS).getNews(symbol, limit);
    }

    @Override
    public ETFProfile getProfile(String symbol) {
        return first(DataCapability.ETF_PROFILE).getEtfProfile(symbol);
    }

    @Override
    public FinancialStatements getFinancialStatements(String symbol) {
        return first(DataCapability.FINANCIAL_STATEMENTS).getFinancialStatements(symbol);
    }

    @Override
    public PriceHistory getPriceHistory(String symbol) {
        return first(DataCapability.PRICE_HISTORY).getPriceHistory(symbol);
    }

    @Override
    public SentimentReading getSentiment(String symbol) {
        return first(DataCapability.SENTIMENT).getSentiment(symbol);
    }

    private DataProvider first(DataCapability capability) {
        List<DataProvider> ordered = routes.get(capability);
        if (ordered == null || ordered.isEmpty()) {
            throw new MarketDataException(missing(capability));
        }
        return ordered.getFirst();
    }

    private static String missing(DataCapability capability) {
        return switch (capability) {
            case QUOTE ->
                    "No quote provider is configured. Set FINNHUB_API_KEY, "
                            + "TWELVE_DATA_API_KEY, or ALPHA_VANTAGE_API_KEY.";
            case OVERVIEW ->
                    "No overview provider is configured. "
                            + "Set FINNHUB_API_KEY or ALPHA_VANTAGE_API_KEY.";
            case NEWS ->
                    "No news provider is configured. Set MARKETAUX_API_TOKEN or FINNHUB_API_KEY.";
            case ETF_PROFILE ->
                    "No ETF provider is configured. Set ALPHA_VANTAGE_API_KEY.";
            case FINANCIAL_STATEMENTS ->
                    "No statement provider is configured. Set FMP_API_KEY.";
            case PRICE_HISTORY ->
                    "No price-history provider is configured. Set TWELVE_DATA_API_KEY.";
            case SENTIMENT ->
                    "No sentiment provider is configured. Set SENTISENSE_API_KEY.";
        };
    }
}
