package com.marketintel.gui;

import com.marketintel.model.MarketQuote;
import com.marketintel.model.NewsArticle;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Pieces of the scrolling desk tape. Prices are formatted, not recalculated.
 * Up and down quotes keep separate colors.
 */
public final class TapeLine {

    public enum Tone {
        UP,
        DOWN,
        FLAT,
        NEWS
    }

    public record Piece(String text, Tone tone) {
    }

    private TapeLine() {
    }

    public static String format(List<MarketQuote> quotes, List<NewsArticle> articles) {
        StringBuilder text = new StringBuilder();
        for (Piece piece : pieces(quotes, articles)) {
            if (!text.isEmpty()) {
                text.append("     ·     ");
            }
            text.append(piece.text());
        }
        if (text.isEmpty()) {
            return "MARKET TAPE  ·  QUOTES AND NEWS ARE UNAVAILABLE";
        }
        return text.toString();
    }

    public static List<Piece> pieces(List<MarketQuote> quotes, List<NewsArticle> articles) {
        List<Piece> pieces = new ArrayList<>();
        if (quotes != null) {
            for (MarketQuote quote : quotes) {
                Tone tone = switch (quote.getPercentChange().signum()) {
                    case 1 -> Tone.UP;
                    case -1 -> Tone.DOWN;
                    default -> Tone.FLAT;
                };
                pieces.add(new Piece(
                        quote.getSymbol()
                                + "  "
                                + plain(quote.getPrice())
                                + "  "
                                + signed(quote.getPercentChange())
                                + "%",
                        tone
                ));
            }
        }
        if (articles != null) {
            int shown = 0;
            for (NewsArticle article : articles) {
                if (article.getTitle() == null || article.getTitle().isBlank() || shown == 4) {
                    continue;
                }
                String source = article.getSource() == null || article.getSource().isBlank()
                        ? "NEWS"
                        : article.getSource();
                pieces.add(new Piece(source + "  " + article.getTitle().trim(), Tone.NEWS));
                shown++;
            }
        }
        return List.copyOf(pieces);
    }

    public static Color color(Tone tone) {
        return switch (tone) {
            case UP -> UiTheme.SUCCESS;
            case DOWN -> UiTheme.DANGER;
            case FLAT -> UiTheme.TEXT;
            case NEWS -> UiTheme.ACCENT;
        };
    }

    private static String plain(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String signed(BigDecimal percent) {
        String text = percent.setScale(2, RoundingMode.HALF_UP).toPlainString();
        if (percent.signum() > 0) {
            return "+" + text;
        }
        return text;
    }
}
