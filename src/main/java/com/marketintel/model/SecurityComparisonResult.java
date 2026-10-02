package com.marketintel.model;

import java.math.BigDecimal;
import java.util.Objects;

public class SecurityComparisonResult {

    private final MarketQuote firstQuote;

    private final MarketQuote secondQuote;

    /*
     * first security % change
     * minus
     * second security % change
     */
    private final BigDecimal percentChangeDifference;

    public SecurityComparisonResult(
            MarketQuote firstQuote,
            MarketQuote secondQuote,
            BigDecimal percentChangeDifference) {

        this.firstQuote =
                Objects.requireNonNull(
                        firstQuote,
                        "First quote cannot be null."
                );

        this.secondQuote =
                Objects.requireNonNull(
                        secondQuote,
                        "Second quote cannot be null."
                );

        this.percentChangeDifference =
                Objects.requireNonNull(
                        percentChangeDifference,
                        "Percent-change difference cannot be null."
                );
    }

    public MarketQuote getFirstQuote() {
        return firstQuote;
    }

    public MarketQuote getSecondQuote() {
        return secondQuote;
    }

    public BigDecimal getPercentChangeDifference() {
        return percentChangeDifference;
    }
}