package com.marketintel.model;

import java.math.BigDecimal;
import java.util.Objects;

public class PortfolioPosition {

    private final String symbol;
    private final BigDecimal quantity;
    private final AssetType assetType;

    public PortfolioPosition(
            String symbol,
            BigDecimal quantity,
            AssetType assetType) {

        this.symbol = Objects.requireNonNull(symbol);
        this.quantity = Objects.requireNonNull(quantity);
        this.assetType = Objects.requireNonNull(assetType);
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
}