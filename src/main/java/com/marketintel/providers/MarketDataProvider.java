package com.marketintel.providers;

import com.marketintel.model.MarketQuote;
import com.marketintel.model.SecurityOverview;

public interface MarketDataProvider {

    MarketQuote getQuote(String symbol);

    SecurityOverview getSecurityOverview(String symbol);
}