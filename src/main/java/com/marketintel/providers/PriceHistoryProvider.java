package com.marketintel.providers;

import com.marketintel.model.PriceHistory;

public interface PriceHistoryProvider {

    PriceHistory getPriceHistory(String symbol);
}
