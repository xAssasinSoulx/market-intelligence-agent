package com.marketintel.services;

import com.marketintel.model.MarketQuote;
import com.marketintel.model.SecurityComparisonResult;

import java.math.BigDecimal;
import java.util.Objects;

public class SecurityComparisonCalculator {

    public SecurityComparisonResult compare(
            MarketQuote firstQuote,
            MarketQuote secondQuote) {

        Objects.requireNonNull(
                firstQuote,
                "First quote cannot be null."
        );

        Objects.requireNonNull(
                secondQuote,
                "Second quote cannot be null."
        );

        BigDecimal difference =
                firstQuote
                        .getPercentChange()
                        .subtract(
                                secondQuote
                                        .getPercentChange()
                        );

        return new SecurityComparisonResult(
                firstQuote,
                secondQuote,
                difference
        );
    }
}