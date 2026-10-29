package com.marketintel.providers;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Fund-level country weights published by a market-data provider.
 * The map values are fractions of the fund, such as 0.60 for 60 percent.
 */
public interface CountryExposureSource {

    Map<String, BigDecimal> getCountryExposure(String symbol);
}
