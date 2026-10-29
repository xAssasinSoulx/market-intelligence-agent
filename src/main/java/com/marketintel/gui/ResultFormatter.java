package com.marketintel.gui;

import com.marketintel.app.MovementReport;
import com.marketintel.model.ExposureResult;
import com.marketintel.model.MarketMovementContext;
import com.marketintel.model.MarketQuote;
import com.marketintel.model.NewsArticle;
import com.marketintel.model.NewsResult;
import com.marketintel.model.OverlapResult;
import com.marketintel.model.PortfolioExposureResult;
import com.marketintel.model.SecurityComparisonResult;
import com.marketintel.model.SecurityOverview;
import com.marketintel.model.ToolResult;
import com.marketintel.model.FinancialStatements;
import com.marketintel.model.PriceHistory;
import com.marketintel.model.SentimentReading;
import com.marketintel.services.InsightReport;
import com.marketintel.services.NewsTiming;
import com.marketintel.services.PortfolioExposureReport;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.Map;

/**
 * Plain-text presentation of tool results. Financial values are formatted
 * here and are not recalculated.
 */
public final class ResultFormatter {

    private ResultFormatter() {
    }

    public static String quote(ToolResult result) {
        if (!result.isSuccess()) {
            return error(result.getErrorMessage());
        }
        if (!(result.getData() instanceof MarketQuote quote)) {
            return error("Unexpected market-data response.");
        }
        return section("MARKET QUOTE")
                + row("Symbol", quote.getSymbol())
                + row("Price", plain(quote.getPrice()))
                + row("Previous close", plain(quote.getPreviousClose()))
                + row("Change", change(quote.getPercentChange()))
                + row(
                "Last trade (UTC)",
                quote.getLastTradeAt().map(NewsTiming::formatUtc).orElse("not reported")
        );
    }

    public static String overview(ToolResult result) {
        if (!result.isSuccess()) {
            return error(result.getErrorMessage());
        }
        if (!(result.getData() instanceof SecurityOverview overview)) {
            return error("Unexpected security overview response.");
        }

        StringBuilder text = new StringBuilder();
        text.append(section("SECURITY OVERVIEW"));
        text.append(row("Symbol", overview.getSymbol()));
        text.append(row("Name", overview.getName()));
        text.append(row("Exchange", overview.getExchange()));
        text.append(row("Currency", overview.getCurrency()));
        text.append(row("Sector", overview.getSector()));
        text.append(row("Industry", overview.getIndustry()));
        if (overview.getMarketCapitalization() != null) {
            text.append(row(
                    "Market cap",
                    plain(overview.getMarketCapitalization())
            ));
        }
        if (overview.getDescription() != null
                && !overview.getDescription().isBlank()) {
            text.append("\n").append(overview.getDescription().trim()).append("\n");
        }
        return text.toString();
    }

    public static String comparison(ToolResult result) {
        if (!result.isSuccess()) {
            return error(result.getErrorMessage());
        }
        if (!(result.getData() instanceof SecurityComparisonResult comparison)) {
            return error("Unexpected comparison response.");
        }

        MarketQuote first = comparison.getFirstQuote();
        MarketQuote second = comparison.getSecondQuote();

        return section("SECURITY COMPARISON")
                + row("First", first.getSymbol()
                + "  " + plain(first.getPrice())
                + "  " + change(first.getPercentChange()))
                + row("Second", second.getSymbol()
                + "  " + plain(second.getPrice())
                + "  " + change(second.getPercentChange()))
                + row(
                "Change difference",
                change(comparison.getPercentChangeDifference())
        )
                + "\nDifference is the first percentage change minus the second. "
                + "It is not a recommendation.\n";
    }

    public static String sectorExposure(ToolResult result) {
        return exposure(
                result,
                "ETF SECTOR EXPOSURE",
                "Weights are the provider allocation, shown as percentages."
        );
    }

    public static String geographicExposure(ToolResult result) {
        return exposure(result, "ETF COUNTRY EXPOSURE", null);
    }

    private static String exposure(ToolResult result, String title, String fallbackNote) {
        if (!result.isSuccess()) {
            return error(result.getErrorMessage());
        }
        if (!(result.getData() instanceof ExposureResult exposure)) {
            return error("Unexpected exposure response.");
        }

        StringBuilder text = new StringBuilder();
        text.append(section(title));
        text.append(row("Symbol", exposure.getSymbol()));

        if (exposure.getCategories().isEmpty()) {
            text.append("No weights were returned.\n");
            return text.toString();
        }

        exposure.getCategories().entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue(
                        Comparator.reverseOrder()
                ))
                .forEach(entry -> text.append(String.format(
                        "%-18s %7s  %s%n",
                        entry.getKey(),
                        percent(entry.getValue()),
                        bar(entry.getValue())
                )));

        String note = exposure.getNote();
        if (note == null || note.isBlank()) {
            note = fallbackNote;
        }
        if (note != null && !note.isBlank()) {
            text.append("\n").append(note).append("\n");
        }
        return text.toString();
    }

    public static String overlap(ToolResult result) {
        if (!result.isSuccess()) {
            return error(result.getErrorMessage());
        }
        if (!(result.getData() instanceof OverlapResult overlap)) {
            return error("Unexpected overlap response.");
        }

        StringBuilder text = new StringBuilder();
        text.append(section("ETF HOLDING OVERLAP"));
        text.append(row("First ETF", overlap.getFirstSymbol()));
        text.append(row("Second ETF", overlap.getSecondSymbol()));
        text.append(row("Overlap", percent(overlap.getOverlapWeight())));
        text.append("\nShared holdings reflect only the holdings the provider returned.\n\n");

        if (overlap.getCommonHoldings().isEmpty()) {
            text.append("No shared holdings were found in the returned data.\n");
            return text.toString();
        }

        overlap.getCommonHoldings().forEach(holding -> text.append(String.format(
                "%-12s %7s  %s%n",
                holding.getSymbol() == null ? "UNKNOWN" : holding.getSymbol(),
                percent(holding.getWeight()),
                holding.getDescription() == null ? "" : holding.getDescription()
        )));
        return text.toString();
    }

    public static String news(ToolResult result) {
        if (!result.isSuccess()) {
            return error(result.getErrorMessage());
        }
        if (!(result.getData() instanceof NewsResult news)) {
            return error("Unexpected news response.");
        }

        StringBuilder text = new StringBuilder();
        text.append(section("FINANCIAL NEWS"));
        text.append(row("Symbol", news.getSymbol()));

        if (news.getArticles().isEmpty()) {
            text.append("No qualifying recent articles were returned.\n");
            return text.toString();
        }

        int index = 1;
        for (NewsArticle article : news.getArticles()) {
            text.append("\n[").append(index).append("] ")
                    .append(article.getTitle()).append("\n");
            text.append(row("Source", article.getSource()));
            text.append(row("Published", article.getPublishedAt()));
            text.append(row("URL", article.getUrl()));
            if (article.getSummary() != null && !article.getSummary().isBlank()) {
                text.append(article.getSummary().trim()).append("\n");
            }
            if (article.getSentimentLabel() != null
                    || article.getSentimentScore() != null) {
                text.append(row(
                        "Sentiment",
                        (article.getSentimentLabel() == null
                                ? "unlabeled"
                                : article.getSentimentLabel())
                                + (article.getSentimentScore() == null
                                ? ""
                                : " (" + plain(article.getSentimentScore()) + ")")
                ));
                text.append("Sentiment is a provider estimate, not evidence that the article moved the price.\n");
            }
            index++;
        }
        return text.toString();
    }

    public static String movement(MovementReport report) {
        if (!report.hasEvidence()) {
            return error(report.getFailureMessage());
        }

        MarketMovementContext context = report.getContext();
        MarketQuote quote = context.getQuote();

        StringBuilder text = new StringBuilder();
        text.append(section("MARKET MOVEMENT"));
        text.append(row("Symbol", context.getSymbol()));
        text.append(row("Price", plain(quote.getPrice())));
        text.append(row("Previous close", plain(quote.getPreviousClose())));
        text.append(row("Change", change(quote.getPercentChange())));
        text.append(row("Retrieved (UTC)", context.getRetrievedAt().toString()));
        text.append(row(
                "Last trade (UTC)",
                quote.getLastTradeAt().map(NewsTiming::formatUtc).orElse("unknown")
        ));
        text.append("\n");
        text.append("The retrieval time is not the trading time. ");
        if (quote.getLastTradeAt().isPresent()) {
            text.append("The last-trade time comes from the quote provider. ");
            text.append("Articles marked below came out after it and cannot have caused this move. ");
        } else {
            text.append("This provider did not report a last-trade time, so news may have been published after the price movement. ");
        }
        text.append("This view does not establish causation and is not investment advice.\n");

        if (context.getNewsWarning() != null && !context.getNewsWarning().isBlank()) {
            text.append("\nNews warning: ").append(context.getNewsWarning()).append("\n");
        }

        text.append(section("NEWS USED AS EVIDENCE"));
        if (context.getArticles().isEmpty()) {
            text.append("No qualifying articles were available.\n");
        } else {
            int index = 1;
            for (var article : context.getArticles()) {
                text.append("[").append(index).append("] ")
                        .append(article.getTitle()).append("\n");
                text.append(row("Source", article.getSource()));
                text.append(row(
                        "Published",
                        article.getPublishedAt()
                                + (NewsTiming.publishedAfterLastTrade(article, quote)
                                ? "  (after the last trade)"
                                : "")
                ));
                text.append(row("URL", article.getUrl()));
                index++;
            }
        }

        text.append(section("EXPLANATION"));
        if (report.getExplanation() != null) {
            text.append(report.getExplanation().trim()).append("\n\n");
            text.append("Citation markers such as [1] can only refer to the numbered sources above. ");
            text.append("A marker does not prove the source supports every sentence.\n");
        } else if (report.getExplanationNote() != null) {
            text.append(report.getExplanationNote().trim()).append("\n");
            text.append("The quote and news evidence above remain available.\n");
        } else {
            text.append("No explanation was produced.\n");
        }
        return text.toString();
    }

    public static String statements(ToolResult result) {
        if (!result.isSuccess()) {
            return error(result.getErrorMessage());
        }
        if (!(result.getData() instanceof FinancialStatements statements)) {
            return error("Unexpected financial-statement response.");
        }
        return InsightReport.statements(statements);
    }

    public static String priceHistory(ToolResult result) {
        if (!result.isSuccess()) {
            return error(result.getErrorMessage());
        }
        if (!(result.getData() instanceof PriceHistory history)) {
            return error("Unexpected price-history response.");
        }
        return InsightReport.priceHistory(history);
    }

    public static String sentiment(ToolResult result) {
        if (!result.isSuccess()) {
            return error(result.getErrorMessage());
        }
        if (!(result.getData() instanceof SentimentReading reading)) {
            return error("Unexpected sentiment response.");
        }
        return InsightReport.sentiment(reading);
    }

    public static String portfolioExposure(ToolResult result) {
        if (!result.isSuccess()) {
            return error(result.getErrorMessage());
        }
        if (!(result.getData() instanceof PortfolioExposureResult exposure)) {
            return error("Unexpected portfolio exposure response.");
        }
        return PortfolioExposureReport.format(exposure);
    }

    public static String unavailable(String message) {
        return error(message);
    }

    private static String error(String message) {
        if (message == null || message.isBlank()) {
            return "Error: The request could not be completed.\n";
        }
        return "Error: " + message.trim() + "\n";
    }

    private static String section(String title) {
        return title + "\n" + "-".repeat(title.length()) + "\n";
    }

    private static String row(String label, String value) {
        String shown = value == null || value.isBlank() ? "Unknown" : value;
        return label + ": " + shown + "\n";
    }

    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private static String change(BigDecimal percent) {
        String text = plain(percent) + "%";
        if (percent.compareTo(BigDecimal.ZERO) > 0) {
            return "+" + text;
        }
        return text;
    }

    private static String percent(BigDecimal fraction) {
        return fraction.multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP)
                .toPlainString() + "%";
    }

    private static String bar(BigDecimal fraction) {
        int units = fraction.multiply(new BigDecimal("100"))
                .divide(new BigDecimal("5"), 0, RoundingMode.HALF_UP)
                .intValue();
        units = Math.max(0, Math.min(units, 20));
        return "|".repeat(units);
    }
}
