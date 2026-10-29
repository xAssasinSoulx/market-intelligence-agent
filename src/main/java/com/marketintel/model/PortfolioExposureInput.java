package com.marketintel.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * One portfolio position plus the provider data needed to value it.
 * Prices and sector weights are already fetched. This object does not
 * call a provider.
 */
public final class PortfolioExposureInput {

    private final String symbol;
    private final BigDecimal quantity;
    private final AssetType assetType;
    private final BigDecimal price;
    private final String currency;
    private final Map<String, BigDecimal> sectorWeights;
    private final List<ETFHolding> holdings;
    private final String note;

    public PortfolioExposureInput(
            String symbol,
            BigDecimal quantity,
            AssetType assetType,
            BigDecimal price,
            String currency,
            Map<String, BigDecimal> sectorWeights,
            List<ETFHolding> holdings,
            String note) {

        this.symbol = symbol;
        this.quantity = quantity;
        this.assetType = assetType;
        this.price = price;
        this.currency = currency;
        this.sectorWeights = sectorWeights == null
                ? Map.of()
                : Map.copyOf(sectorWeights);
        this.holdings = holdings == null
                ? List.of()
                : List.copyOf(holdings);
        this.note = note;
    }

    public String getSymbol() {
        return symbol;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public AssetType getAssetType() {
        return assetType;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }

    public Map<String, BigDecimal> getSectorWeights() {
        return sectorWeights;
    }

    public List<ETFHolding> getHoldings() {
        return holdings;
    }

    public String getNote() {
        return note;
    }
}
