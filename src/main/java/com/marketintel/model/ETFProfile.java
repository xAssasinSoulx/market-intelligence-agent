package com.marketintel.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ETFProfile {

    private final String symbol;
    private final BigDecimal netAssets;
    private final BigDecimal expenseRatio;

    private final List<ETFHolding> holdings;

    private final Map<String, BigDecimal>
            sectorWeights;

    public ETFProfile(
            String symbol,
            BigDecimal netAssets,
            BigDecimal expenseRatio,
            List<ETFHolding> holdings,
            Map<String, BigDecimal> sectorWeights) {

        this.symbol = symbol;
        this.netAssets = netAssets;
        this.expenseRatio = expenseRatio;

        this.holdings =
                new ArrayList<>(holdings);

        this.sectorWeights =
                new LinkedHashMap<>(
                        sectorWeights
                );
    }

    public String getSymbol() {
        return symbol;
    }

    public BigDecimal getNetAssets() {
        return netAssets;
    }

    public BigDecimal getExpenseRatio() {
        return expenseRatio;
    }

    public List<ETFHolding> getHoldings() {

        return Collections.unmodifiableList(
                holdings
        );
    }

    public Map<String, BigDecimal>
    getSectorWeights() {

        return Collections.unmodifiableMap(
                sectorWeights
        );
    }
}