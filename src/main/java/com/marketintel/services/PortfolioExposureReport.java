package com.marketintel.services;

import com.marketintel.model.PortfolioExposureResult;
import com.marketintel.model.PortfolioExposureResult.CurrencyBook;
import com.marketintel.model.PortfolioExposureResult.HoldingLine;
import com.marketintel.model.PortfolioExposureResult.PositionLine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Plain-text portfolio exposure. The terminal and the desk both use this
 * so the figures are not formatted twice.
 */
public final class PortfolioExposureReport {

    private PortfolioExposureReport() {
    }

    public static String format(PortfolioExposureResult result) {
        StringBuilder text = new StringBuilder();
        text.append("PORTFOLIO EXPOSURE\n");
        text.append("------------------\n");
        text.append("Market value is quantity times the latest price.\n");
        text.append("Share count is not used as value.\n");
        text.append("Amounts in different currencies are not added together.\n\n");

        if (result.getPositions().isEmpty()) {
            text.append("No positions.\n");
        }

        for (PositionLine line : result.getPositions()) {
            text.append(line.getSymbol())
                    .append("  ")
                    .append(line.getAssetType().name())
                    .append("  ");
            if (!line.isIncluded()) {
                text.append("excluded");
                if (line.getPrice() == null) {
                    text.append(", no positive price");
                }
                text.append('\n');
                continue;
            }
            text.append(plain(line.getQuantity()))
                    .append(" x ")
                    .append(plain(line.getPrice()))
                    .append(' ')
                    .append(line.getCurrency())
                    .append(" = ")
                    .append(plain(line.getMarketValue()))
                    .append('\n');
        }

        for (CurrencyBook book : result.getBooks()) {
            text.append("\n").append(book.getCurrency()).append('\n');
            text.append("Total market value: ")
                    .append(plain(book.getTotalMarketValue()))
                    .append('\n');
            text.append("Sector weights are a share of this currency book.\n");
            for (Map.Entry<String, BigDecimal> entry : book.getSectorWeights().entrySet()) {
                text.append(String.format(
                        "%-18s %s%n",
                        entry.getKey(),
                        percent(entry.getValue())
                ));
            }
            if (book.getLookThrough().isEmpty()) {
                continue;
            }
            text.append("ETF look-through, not added to direct stock positions:\n");
            for (HoldingLine holding : book.getLookThrough()) {
                text.append(String.format(
                        "%-12s %8s  via %s%n",
                        holding.getSymbol(),
                        percent(holding.getPortfolioWeight()),
                        holding.getSource()
                ));
            }
        }

        if (!result.getLimitations().isEmpty()) {
            text.append("\nLimitations\n");
            for (String limitation : result.getLimitations()) {
                text.append("- ").append(limitation).append('\n');
            }
        }
        return text.toString();
    }

    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private static String percent(BigDecimal fraction) {
        return fraction.multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP)
                .toPlainString() + "%";
    }
}
