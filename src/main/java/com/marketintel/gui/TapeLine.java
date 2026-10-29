package com.marketintel.gui;

import com.marketintel.model.MarketQuote;
import com.marketintel.model.NewsArticle;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * One line for the scrolling desk tape. Prices are formatted, not recalculated.
 */
public final class TapeLine {

    private TapeLine() {
    }

    public static String format(List<MarketQuote> quotes, List<NewsArticle> articles) {
        StringBuilder text = new StringBuilder();
        if (quotes != null) {
            for (MarketQuote quote : quotes) {
                separate(text);
                text.append(quote.getSymbol())
                        .append("  ")
                        .append(plain(quote.getPrice()))
                        .append("  ")
                        .append(signed(quote.getPercentChange()))
                        .append("%");
            }
        }
        if (articles != null) {
            int shown = 0;
            for (NewsArticle article : articles) {
                if (article.getTitle() == null || article.getTitle().isBlank()) {
                    continue;
                }
                if (shown == 4) {
                    break;
                }
                separate(text);
                String source = article.getSource() == null || article.getSource().isBlank()
                        ? "NEWS"
                        : article.getSource();
                text.append(source).append("  ").append(article.getTitle().trim());
                shown++;
            }
        }
        if (text.isEmpty()) {
            return "MARKET TAPE  ·  QUOTES AND NEWS ARE UNAVAILABLE";
        }
        return text.toString();
    }

    private static void separate(StringBuilder text) {
        if (!text.isEmpty()) {
            text.append("     ·     ");
        }
    }

    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private static String signed(BigDecimal percent) {
        String text = percent.setScale(2, RoundingMode.HALF_UP).toPlainString();
        if (percent.signum() > 0) {
            return "+" + text;
        }
        return text;
    }
}
