package com.marketintel.providers;

/**
 * A kind of data one adapter can serve. Routing policy, which adapter
 * runs first, lives on {@link ProviderRouter}, not on this list.
 */
public enum DataCapability {
    QUOTE,
    OVERVIEW,
    NEWS,
    ETF_PROFILE,
    FINANCIAL_STATEMENTS,
    PRICE_HISTORY,
    SENTIMENT
}
