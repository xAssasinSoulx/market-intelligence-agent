package com.marketintel.providers;

import com.marketintel.model.ETFProfile;

public interface ETFDataProvider {

    ETFProfile getProfile(String symbol);
}