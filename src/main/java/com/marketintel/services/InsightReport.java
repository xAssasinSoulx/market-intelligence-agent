package com.marketintel.services;

import com.marketintel.model.FinancialStatements;
import com.marketintel.model.PriceBar;
import com.marketintel.model.PriceHistory;
import com.marketintel.model.ReportingPeriod;
import com.marketintel.model.SentimentReading;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Plain-text provider reports. The terminal and the desk both use this
 * so a figure is formatted once.
 */
public final class InsightReport {

    private static final int STATEMENT_ROWS = 4;
    private static final int PRICE_ROWS = 8;

    private InsightReport() {
    }

    public static String statements(FinancialStatements statements) {
        StringBuilder text = new StringBuilder();
        text.append(section("FINANCIAL STATEMENTS"));
        text.append(row("Source", statements.getSource()));
        text.append(row("Symbol", statements.getSymbol()));
        text.append(row("Currency", statements.getCurrency()));
        if (!statements.getNote().isBlank()) {
            text.append(statements.getNote().trim()).append("\n\n");
        }
        List<ReportingPeriod> periods = statements.getPeriods();
        if (periods.isEmpty()) {
            text.append("No periods were returned.\n");
            return text.toString();
        }
        int shown = 0;
        for (ReportingPeriod period : periods) {
            if (shown == STATEMENT_ROWS) {
                break;
            }
            String label = period.getPeriodLabel() == null || period.getPeriodLabel().isBlank()
                    ? period.getDate()
                    : period.getDate() + "  " + period.getPeriodLabel();
            text.append(label).append('\n');
            line(text, "Revenue", period.getRevenue());
            line(text, "Gross profit", period.getGrossProfit());
            line(text, "Operating income", period.getOperatingIncome());
            line(text, "Net income", period.getNetIncome());
            line(text, "Diluted EPS", period.getDilutedEps());
            line(text, "Total assets", period.getTotalAssets());
            line(text, "Total liabilities", period.getTotalLiabilities());
            line(text, "Equity", period.getTotalEquity());
            line(text, "Cash", period.getCash());
            line(text, "Operating cash flow", period.getOperatingCashFlow());
            line(text, "Free cash flow", period.getFreeCashFlow());
            text.append('\n');
            shown++;
        }
        return text.toString();
    }

    public static String priceHistory(PriceHistory history) {
        StringBuilder text = new StringBuilder();
        text.append(section("PRICE HISTORY"));
        text.append(row("Source", history.getSource()));
        text.append(row("Symbol", history.getSymbol()));
        text.append(row("Interval", history.getInterval()));
        text.append(row("Currency", history.getCurrency()));
        text.append(row(
                "RSI 14",
                history.getLatestRsi() == null
                        ? "not returned"
                        : plain(history.getLatestRsi().setScale(2, RoundingMode.HALF_UP))
        ));
        if (!history.getNote().isBlank()) {
            text.append(history.getNote().trim()).append("\n\n");
        }
        int shown = 0;
        for (PriceBar bar : history.getBars()) {
            if (shown == PRICE_ROWS) {
                break;
            }
            text.append(bar.getDate())
                    .append("  O ").append(plain(bar.getOpen()))
                    .append("  H ").append(plain(bar.getHigh()))
                    .append("  L ").append(plain(bar.getLow()))
                    .append("  C ").append(plain(bar.getClose()))
                    .append("  V ").append(plain(bar.getVolume()))
                    .append('\n');
            shown++;
        }
        if (history.getBars().size() > PRICE_ROWS) {
            text.append("Showing ").append(PRICE_ROWS)
                    .append(" of ").append(history.getBars().size())
                    .append(" bars.\n");
        }
        return text.toString();
    }

    public static String sentiment(SentimentReading reading) {
        StringBuilder text = new StringBuilder();
        text.append(section("NEWS SENTIMENT"));
        text.append(row("Source", reading.getSource()));
        text.append(row("Symbol", reading.getSymbol()));
        text.append(row("Score", plain(reading.getScore())));
        text.append(row("Label", reading.getLabel()));
        text.append(row(
                "Change",
                reading.getChange() == null ? "not available" : signed(reading.getChange())
        ));
        text.append(row("As of", reading.getAsOf()));
        if (!reading.getNote().isBlank()) {
            text.append('\n').append(reading.getNote().trim()).append('\n');
        }
        return text.toString();
    }

    private static void line(StringBuilder text, String label, BigDecimal value) {
        if (value != null) {
            text.append("  ").append(label).append(": ").append(plain(value)).append('\n');
        }
    }

    private static String section(String title) {
        return title + "\n" + "-".repeat(title.length()) + "\n";
    }

    private static String row(String label, String value) {
        String shown = value == null || value.isBlank() ? "not reported" : value;
        return label + ": " + shown + "\n";
    }

    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private static String signed(BigDecimal value) {
        String text = plain(value);
        if (value.signum() > 0) {
            return "+" + text;
        }
        return text;
    }
}
