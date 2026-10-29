package com.marketintel.providers;

import com.marketintel.model.ETFProfile;
import com.marketintel.model.FinancialStatements;
import com.marketintel.model.MarketQuote;
import com.marketintel.model.NewsArticle;
import com.marketintel.model.PriceHistory;
import com.marketintel.model.SecurityOverview;
import com.marketintel.model.SentimentReading;

import java.util.List;
import java.util.Optional;

/**
 * Target of the Adapter pattern.
 *
 * Tools never call a vendor URL. Each adapter translates one vendor's
 * payload into these domain types and reports the capabilities its plan
 * actually serves. A call the adapter does not implement fails here, so
 * a missing plan is visible instead of returning an empty success.
 */
public interface DataProvider {

    String providerName();

    boolean supports(DataCapability capability);

    /**
     * Currency of every quote this adapter can return, when its plan is
     * limited to one market. Empty when the adapter serves several
     * currencies or does not say.
     */
    default Optional<String> quoteCurrency() {
        return Optional.empty();
    }

    default MarketQuote getQuote(String symbol) {
        throw unsupported(DataCapability.QUOTE);
    }

    default SecurityOverview getSecurityOverview(String symbol) {
        throw unsupported(DataCapability.OVERVIEW);
    }

    default List<NewsArticle> getNews(String symbol, int limit) {
        throw unsupported(DataCapability.NEWS);
    }

    default ETFProfile getEtfProfile(String symbol) {
        throw unsupported(DataCapability.ETF_PROFILE);
    }

    default FinancialStatements getFinancialStatements(String symbol) {
        throw unsupported(DataCapability.FINANCIAL_STATEMENTS);
    }

    default PriceHistory getPriceHistory(String symbol) {
        throw unsupported(DataCapability.PRICE_HISTORY);
    }

    default SentimentReading getSentiment(String symbol) {
        throw unsupported(DataCapability.SENTIMENT);
    }

    private MarketDataException unsupported(DataCapability capability) {
        return new MarketDataException(
                providerName() + " does not serve " + capability + "."
        );
    }
}
