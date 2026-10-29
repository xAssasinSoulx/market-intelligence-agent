package com.marketintel.services;

import com.marketintel.model.MarketQuote;
import com.marketintel.model.NewsArticle;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * Compares article publication times with the quote's last trade.
 * An article published after the last trade cannot have caused it.
 */
public final class NewsTiming {

    private static final DateTimeFormatter UTC =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'")
                    .withZone(ZoneOffset.UTC);

    private NewsTiming() {
    }

    public static String formatUtc(Instant instant) {
        return UTC.format(instant);
    }

    /**
     * Parses the provider timestamp. Marketaux uses ISO-8601. The older
     * Alpha Vantage format, 20261018T142500, is also accepted.
     */
    public static Optional<Instant> parsePublishedAt(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            return Optional.empty();
        }

        String text = timestamp.trim();
        try {
            return Optional.of(Instant.parse(text));
        } catch (DateTimeParseException ignored) {
            // Not ISO-8601 with a zone. Try the compact form below.
        }

        if (text.matches("\\d{8}T\\d{6}")) {
            try {
                return Optional.of(Instant.parse(
                        text.substring(0, 4) + "-" + text.substring(4, 6)
                                + "-" + text.substring(6, 8)
                                + "T" + text.substring(9, 11)
                                + ":" + text.substring(11, 13)
                                + ":" + text.substring(13, 15) + "Z"
                ));
            } catch (DateTimeParseException ignored) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    /**
     * True only when both times are known and the article came out
     * after the last trade in the quote.
     */
    public static boolean publishedAfterLastTrade(
            NewsArticle article,
            MarketQuote quote) {

        Optional<Instant> lastTrade = quote.getLastTradeAt();
        if (lastTrade.isEmpty()) {
            return false;
        }
        return parsePublishedAt(article.getPublishedAt())
                .map(published -> published.isAfter(lastTrade.get()))
                .orElse(false);
    }
}
