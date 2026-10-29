package com.marketintel.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Market-value exposure for one signed-in portfolio.
 * Currency books are separate. Share quantity is not a weight.
 */
public final class PortfolioExposureResult {

    private final List<PositionLine> positions;
    private final List<CurrencyBook> books;
    private final List<String> limitations;

    public PortfolioExposureResult(
            List<PositionLine> positions,
            List<CurrencyBook> books,
            List<String> limitations) {

        this.positions = List.copyOf(positions);
        this.books = List.copyOf(books);
        this.limitations = List.copyOf(limitations);
    }

    public List<PositionLine> getPositions() {
        return positions;
    }

    public List<CurrencyBook> getBooks() {
        return books;
    }

    public List<String> getLimitations() {
        return limitations;
    }

    public static final class PositionLine {

        private final String symbol;
        private final AssetType assetType;
        private final BigDecimal quantity;
        private final BigDecimal price;
        private final String currency;
        private final BigDecimal marketValue;
        private final boolean included;

        public PositionLine(
                String symbol,
                AssetType assetType,
                BigDecimal quantity,
                BigDecimal price,
                String currency,
                BigDecimal marketValue,
                boolean included) {

            this.symbol = symbol;
            this.assetType = assetType;
            this.quantity = quantity;
            this.price = price;
            this.currency = currency;
            this.marketValue = marketValue;
            this.included = included;
        }

        public String getSymbol() {
            return symbol;
        }

        public AssetType getAssetType() {
            return assetType;
        }

        public BigDecimal getQuantity() {
            return quantity;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public String getCurrency() {
            return currency;
        }

        public BigDecimal getMarketValue() {
            return marketValue;
        }

        public boolean isIncluded() {
            return included;
        }
    }

    public static final class CurrencyBook {

        private final String currency;
        private final BigDecimal totalMarketValue;
        private final Map<String, BigDecimal> sectorWeights;
        private final List<HoldingLine> lookThrough;

        public CurrencyBook(
                String currency,
                BigDecimal totalMarketValue,
                Map<String, BigDecimal> sectorWeights,
                List<HoldingLine> lookThrough) {

            this.currency = currency;
            this.totalMarketValue = totalMarketValue;
            this.sectorWeights = Map.copyOf(sectorWeights);
            this.lookThrough = List.copyOf(lookThrough);
        }

        public String getCurrency() {
            return currency;
        }

        public BigDecimal getTotalMarketValue() {
            return totalMarketValue;
        }

        public Map<String, BigDecimal> getSectorWeights() {
            return sectorWeights;
        }

        public List<HoldingLine> getLookThrough() {
            return lookThrough;
        }
    }

    public static final class HoldingLine {

        private final String symbol;
        private final BigDecimal portfolioWeight;
        private final String source;

        public HoldingLine(
                String symbol,
                BigDecimal portfolioWeight,
                String source) {

            this.symbol = symbol;
            this.portfolioWeight = portfolioWeight;
            this.source = source;
        }

        public String getSymbol() {
            return symbol;
        }

        public BigDecimal getPortfolioWeight() {
            return portfolioWeight;
        }

        public String getSource() {
            return source;
        }
    }
}
