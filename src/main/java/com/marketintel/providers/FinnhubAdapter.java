package com.marketintel.providers;

import com.marketintel.model.MarketQuote;
import com.marketintel.model.NewsArticle;
import com.marketintel.model.SecurityOverview;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Adapter for the Finnhub REST API.
 *
 * The free plan serves quotes, company profiles, and company news.
 * ETF country weights and some ETF datasets are outside that plan, so
 * this adapter does not claim them. Filings exist on the vendor API and
 * are not called here.
 */
public final class FinnhubAdapter implements DataProvider {

    static final int NEWS_LOOKBACK_DAYS = 7;
    private static final int MAX_NEWS = 20;

    private final FinnhubMarketDataClient client;

    public FinnhubAdapter(FinnhubMarketDataClient client) {
        if (client == null) {
            throw new IllegalArgumentException("Finnhub client cannot be null.");
        }
        this.client = client;
    }

    @Override
    public String providerName() {
        return "Finnhub";
    }

    @Override
    public boolean supports(DataCapability capability) {
        return capability == DataCapability.QUOTE
                || capability == DataCapability.OVERVIEW
                || capability == DataCapability.NEWS;
    }

    @Override
    public Optional<String> quoteCurrency() {
        return client.quoteCurrency();
    }

    @Override
    public MarketQuote getQuote(String symbol) {
        return client.getQuote(symbol);
    }

    @Override
    public SecurityOverview getSecurityOverview(String symbol) {
        return client.getSecurityOverview(symbol);
    }

    @Override
    public List<NewsArticle> getNews(String symbol, int limit) {
        String normalized = Symbols.require(symbol);
        if (limit <= 0) {
            throw new IllegalArgumentException("News limit must be positive.");
        }
        int capped = Math.min(limit, MAX_NEWS);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate from = today.minusDays(NEWS_LOOKBACK_DAYS);
        JsonNode root = client.fetchCompanyNews(normalized, from.toString(), today.toString());
        return parseCompanyNews(normalized, root, capped);
    }

    List<NewsArticle> parseCompanyNews(String symbol, JsonNode root, int limit) {
        if (root == null || !root.isArray()) {
            throw new MarketDataException(
                    "Finnhub returned an unreadable company-news list for " + symbol + "."
            );
        }
        List<NewsArticle> articles = new ArrayList<>();
        for (JsonNode node : root) {
            if (articles.size() >= limit) {
                break;
            }
            String headline = JsonValues.text(node, "headline");
            String url = JsonValues.text(node, "url");
            if (headline.isBlank() || url.isBlank()) {
                continue;
            }
            if (!mentions(JsonValues.text(node, "related"), symbol)) {
                continue;
            }
            String publishedAt = null;
            JsonNode timestamp = node.path("datetime");
            if (timestamp.isNumber() && timestamp.asLong() > 0) {
                publishedAt = Instant.ofEpochSecond(timestamp.asLong()).toString();
            }
            String summary = JsonValues.text(node, "summary");
            articles.add(new NewsArticle(
                    headline,
                    url,
                    blankToNull(JsonValues.text(node, "source")),
                    publishedAt,
                    summary.isBlank() ? null : summary,
                    null,
                    null
            ));
        }
        return List.copyOf(articles);
    }

    private static boolean mentions(String related, String symbol) {
        if (related == null || related.isBlank()) {
            return true;
        }
        for (String token : related.split("[,\\s]+")) {
            if (token.equalsIgnoreCase(symbol)) {
                return true;
            }
        }
        return false;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
