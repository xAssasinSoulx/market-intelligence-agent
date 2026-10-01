package com.marketintel.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OverlapResult {

    private final String firstSymbol;
    private final String secondSymbol;

    private final BigDecimal overlapWeight;

    private final List<ETFHolding>
            commonHoldings;

    public OverlapResult(
            String firstSymbol,
            String secondSymbol,
            BigDecimal overlapWeight,
            List<ETFHolding> commonHoldings) {

        this.firstSymbol = firstSymbol;
        this.secondSymbol = secondSymbol;
        this.overlapWeight = overlapWeight;

        this.commonHoldings =
                new ArrayList<>(
                        commonHoldings
                );
    }

    public String getFirstSymbol() {
        return firstSymbol;
    }

    public String getSecondSymbol() {
        return secondSymbol;
    }

    public BigDecimal getOverlapWeight() {
        return overlapWeight;
    }

    public List<ETFHolding>
    getCommonHoldings() {

        return Collections.unmodifiableList(
                commonHoldings
        );
    }
}