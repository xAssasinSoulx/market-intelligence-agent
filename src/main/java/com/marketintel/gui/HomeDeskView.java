package com.marketintel.gui;

import com.marketintel.services.PortfolioBook;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Turns a priced book into the home-screen figures.
 * A book with no priced position stays blank. Zero is reserved for a
 * quote that actually returned a price. Weights use market value.
 * Rows are largest first, then every unpriced symbol.
 */
public final class HomeDeskView {

    public enum Tone {
        UP,
        DOWN,
        FLAT
    }

    public record Row(
            String symbol,
            String weight,
            String value,
            String dayDollars,
            String quotePercent,
            boolean unpriced,
            Tone dayTone,
            Tone quoteTone) {
    }

    public record Segment(String symbol, BigDecimal fraction, Tone tone) {
    }

    public record Model(
            boolean totalsBlank,
            String marketValue,
            String day,
            Tone dayTone,
            String coverage,
            String note,
            List<Row> rows,
            List<Segment> segments) {
    }

    private HomeDeskView() {
    }

    public static String pricingNote(int positions) {
        return "PRICING " + positions + " POSITIONS...";
    }

    public static Model present(PortfolioBook book) {
        List<PortfolioBook.Row> priced = new ArrayList<>(book.priced());
        priced.sort(Comparator.comparing(PortfolioBook.Row::marketValue)
                .reversed()
                .thenComparing(PortfolioBook.Row::symbol));

        int held = priced.size() + book.unpriced().size();
        if (priced.isEmpty()) {
            String note = held == 0
                    ? "NO OPEN POSITIONS."
                    : "NO POSITION COULD BE PRICED.";
            List<Row> rows = new ArrayList<>();
            for (String symbol : book.unpriced()) {
                rows.add(unpriced(symbol));
            }
            return new Model(
                    true,
                    "—",
                    "—",
                    Tone.FLAT,
                    null,
                    note,
                    List.copyOf(rows),
                    List.of()
            );
        }

        BigDecimal total = book.marketValue();
        List<Row> rows = new ArrayList<>();
        List<Segment> segments = new ArrayList<>();
        BigDecimal remaining = BigDecimal.ONE;
        boolean weighted = total.signum() != 0;
        for (int index = 0; index < priced.size(); index++) {
            PortfolioBook.Row row = priced.get(index);
            Tone dayTone = tone(row.dayChange());
            rows.add(new Row(
                    row.symbol(),
                    weight(row.marketValue(), total),
                    money(row.marketValue()),
                    signedMoney(row.dayChange()),
                    signed(row.percentChange()) + "%",
                    false,
                    dayTone,
                    tone(row.percentChange())
            ));
            if (!weighted) {
                continue;
            }
            BigDecimal fraction;
            if (index == priced.size() - 1) {
                fraction = remaining.max(BigDecimal.ZERO);
            } else {
                fraction = row.marketValue().divide(total, 8, RoundingMode.DOWN);
                if (fraction.compareTo(remaining) > 0) {
                    fraction = remaining;
                }
                remaining = remaining.subtract(fraction);
            }
            segments.add(new Segment(row.symbol(), fraction, dayTone));
        }
        for (String symbol : book.unpriced()) {
            rows.add(unpriced(symbol));
        }

        String coverage = book.unpriced().isEmpty()
                ? null
                : priced.size() + " OF " + held + " PRICED";
        String note = book.unpriced().isEmpty()
                ? quantityNote()
                : "Total omits "
                        + String.join(", ", book.unpriced())
                        + ". "
                        + quantityNote();
        return new Model(
                false,
                money(total),
                day(book),
                tone(book.dayChange()),
                coverage,
                note,
                List.copyOf(rows),
                List.copyOf(segments)
        );
    }

    private static Row unpriced(String symbol) {
        return new Row(
                symbol,
                "UNPRICED",
                "",
                "",
                "",
                true,
                Tone.FLAT,
                Tone.FLAT
        );
    }

    private static String quantityNote() {
        return "Quantity times the latest price. Day is the move from the previous close.";
    }

    private static String weight(BigDecimal marketValue, BigDecimal total) {
        if (total.signum() == 0) {
            return "—";
        }
        return marketValue
                .multiply(new BigDecimal("100"))
                .divide(total, 2, RoundingMode.HALF_UP)
                .toPlainString()
                + "%";
    }

    private static String day(PortfolioBook book) {
        String amount = signedMoney(book.dayChange());
        BigDecimal percent = book.dayPercent();
        if (percent == null) {
            return amount;
        }
        return amount + "   (" + signed(percent) + "%)";
    }

    private static Tone tone(BigDecimal value) {
        return switch (value.signum()) {
            case 1 -> Tone.UP;
            case -1 -> Tone.DOWN;
            default -> Tone.FLAT;
        };
    }

    private static String signedMoney(BigDecimal value) {
        String text = money(value.abs());
        if (value.signum() < 0) {
            return "-" + text;
        }
        if (value.signum() > 0) {
            return "+" + text;
        }
        return text;
    }

    private static String money(BigDecimal value) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        format.setGroupingUsed(true);
        return format.format(value);
    }

    private static String signed(BigDecimal value) {
        String text = value.setScale(2, RoundingMode.HALF_UP).toPlainString();
        if (value.signum() > 0 && !text.startsWith("+")) {
            return "+" + text;
        }
        return text;
    }
}
