package com.marketintel.providers;

import com.marketintel.model.ETFProfile;

/**
 * Stands in when no ETF data key is configured. Every request fails with
 * a message that names the missing configuration, so tools that need
 * both quotes and ETF data can still serve the quote half.
 */
public final class UnconfiguredETFDataProvider implements ETFDataProvider {

    @Override
    public ETFProfile getProfile(String symbol) {
        throw new MarketDataException(
                "ETF holdings are unavailable. Set ALPHA_VANTAGE_API_KEY and restart."
        );
    }
}
