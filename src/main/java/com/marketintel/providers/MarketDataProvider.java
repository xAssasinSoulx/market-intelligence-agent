package com.marketintel.providers;

import com.marketintel.model.MarketQuote;
import com.marketintel.model.SecurityOverview;

import java.util.Optional;

public interface MarketDataProvider {

    MarketQuote getQuote(String symbol);

    SecurityOverview getSecurityOverview(String symbol);

    /**
     * Currency of every quote this provider can return, when the plan
     * restricts coverage to one market. Empty when the provider serves
     * several currencies or does not say.
     */
    default Optional<String> quoteCurrency() {
        return Optional.empty();
    }
}