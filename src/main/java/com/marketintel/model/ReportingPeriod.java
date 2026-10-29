package com.marketintel.model;

import java.math.BigDecimal;

/**
 * One reporting period assembled from an income statement, a balance
 * sheet, and a cash-flow statement. A null figure was absent from the
 * provider payload. It is not a zero.
 */
public final class ReportingPeriod {

    private final String date;
    private final String periodLabel;
    private final BigDecimal revenue;
    private final BigDecimal grossProfit;
    private final BigDecimal operatingIncome;
    private final BigDecimal netIncome;
    private final BigDecimal dilutedEps;
    private final BigDecimal totalAssets;
    private final BigDecimal totalLiabilities;
    private final BigDecimal totalEquity;
    private final BigDecimal cash;
    private final BigDecimal operatingCashFlow;
    private final BigDecimal freeCashFlow;

    private ReportingPeriod(Builder builder) {
        this.date = builder.date;
        this.periodLabel = builder.periodLabel;
        this.revenue = builder.revenue;
        this.grossProfit = builder.grossProfit;
        this.operatingIncome = builder.operatingIncome;
        this.netIncome = builder.netIncome;
        this.dilutedEps = builder.dilutedEps;
        this.totalAssets = builder.totalAssets;
        this.totalLiabilities = builder.totalLiabilities;
        this.totalEquity = builder.totalEquity;
        this.cash = builder.cash;
        this.operatingCashFlow = builder.operatingCashFlow;
        this.freeCashFlow = builder.freeCashFlow;
    }

    public String getDate() {
        return date;
    }

    public String getPeriodLabel() {
        return periodLabel;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public BigDecimal getGrossProfit() {
        return grossProfit;
    }

    public BigDecimal getOperatingIncome() {
        return operatingIncome;
    }

    public BigDecimal getNetIncome() {
        return netIncome;
    }

    public BigDecimal getDilutedEps() {
        return dilutedEps;
    }

    public BigDecimal getTotalAssets() {
        return totalAssets;
    }

    public BigDecimal getTotalLiabilities() {
        return totalLiabilities;
    }

    public BigDecimal getTotalEquity() {
        return totalEquity;
    }

    public BigDecimal getCash() {
        return cash;
    }

    public BigDecimal getOperatingCashFlow() {
        return operatingCashFlow;
    }

    public BigDecimal getFreeCashFlow() {
        return freeCashFlow;
    }

    public static Builder builder(String date) {
        return new Builder(date);
    }

    public static final class Builder {

        private final String date;
        private String periodLabel;
        private BigDecimal revenue;
        private BigDecimal grossProfit;
        private BigDecimal operatingIncome;
        private BigDecimal netIncome;
        private BigDecimal dilutedEps;
        private BigDecimal totalAssets;
        private BigDecimal totalLiabilities;
        private BigDecimal totalEquity;
        private BigDecimal cash;
        private BigDecimal operatingCashFlow;
        private BigDecimal freeCashFlow;

        private Builder(String date) {
            if (date == null || date.isBlank()) {
                throw new IllegalArgumentException("Reporting date cannot be blank.");
            }
            this.date = date.trim();
        }

        public Builder periodLabel(String periodLabel) {
            this.periodLabel = periodLabel;
            return this;
        }

        public Builder revenue(BigDecimal revenue) {
            this.revenue = revenue;
            return this;
        }

        public Builder grossProfit(BigDecimal grossProfit) {
            this.grossProfit = grossProfit;
            return this;
        }

        public Builder operatingIncome(BigDecimal operatingIncome) {
            this.operatingIncome = operatingIncome;
            return this;
        }

        public Builder netIncome(BigDecimal netIncome) {
            this.netIncome = netIncome;
            return this;
        }

        public Builder dilutedEps(BigDecimal dilutedEps) {
            this.dilutedEps = dilutedEps;
            return this;
        }

        public Builder totalAssets(BigDecimal totalAssets) {
            this.totalAssets = totalAssets;
            return this;
        }

        public Builder totalLiabilities(BigDecimal totalLiabilities) {
            this.totalLiabilities = totalLiabilities;
            return this;
        }

        public Builder totalEquity(BigDecimal totalEquity) {
            this.totalEquity = totalEquity;
            return this;
        }

        public Builder cash(BigDecimal cash) {
            this.cash = cash;
            return this;
        }

        public Builder operatingCashFlow(BigDecimal operatingCashFlow) {
            this.operatingCashFlow = operatingCashFlow;
            return this;
        }

        public Builder freeCashFlow(BigDecimal freeCashFlow) {
            this.freeCashFlow = freeCashFlow;
            return this;
        }

        public ReportingPeriod build() {
            return new ReportingPeriod(this);
        }
    }
}
