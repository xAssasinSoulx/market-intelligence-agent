package com.marketintel.agent;

import com.marketintel.model.ActionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Maps a question onto an allowlisted action. Pronouns use the last symbol
 * stored for this user. An ambiguous question asks for a symbol instead of
 * inventing one. Instructions hidden in the text are not executed.
 */
public final class RequestPlanner {

    private static final Pattern TICKER = Pattern.compile("^[A-Z][A-Z0-9]{0,4}(?:\\.[A-Z]{1,2})?$");
    private static final Set<String> STOP = Set.of(
            "A", "AN", "AND", "ABOUT", "ASK", "THE", "ITS", "IT", "THAT", "THIS",
            "WHAT", "WHY", "HOW", "DID", "DOES", "DO", "MOVE", "MOVED", "MOVEMENT",
            "SECTOR", "SECTORS", "NEWS", "QUOTE", "PRICE", "OVERVIEW", "COMPARE",
            "OVERLAP", "WITH", "VERSUS", "VS", "GEO", "COUNTRY", "EXPOSURE",
            "GEOGRAPHIC", "GEOGRAPHY", "FINANCIAL", "STATEMENT", "STATEMENTS",
            "HISTORY", "SENTIMENT", "PORTFOLIO", "DAILY", "RSI", "SHOW", "ME",
            "FOR", "OF", "ON", "TO", "PLEASE", "RECENT", "LAST", "STOCK", "ETF",
            "FUND", "SAME", "ONE", "THEM", "THEIR", "MY", "IS", "ARE", "WAS",
            "TELL", "GET", "GIVE", "LOOK", "UP", "INTO", "FROM", "THAN", "BOTH"
    );

    public AgentPlan createPlan(String input, String previousSymbol) {
        if (input == null || input.isBlank()) {
            return AgentPlan.clarify("Ask a question, such as why NVDA moved or what its sector exposure is.");
        }
        String text = input.trim();
        if (text.length() > 500) {
            return AgentPlan.unsupported("That question is too long. Ask about one symbol and one action.");
        }
        String upper = text.toUpperCase(Locale.ROOT);
        if (isInjection(upper)) {
            return AgentPlan.unsupported(
                    "That text is not run as a command. Ask a market question, such as the sector exposure of a symbol."
            );
        }

        ActionType action = intent(upper);
        List<String> symbols = symbols(upper);
        boolean pronoun = refersToPrevious(upper);
        String prior = previousSymbol == null || previousSymbol.isBlank()
                ? null
                : previousSymbol.trim().toUpperCase(Locale.ROOT);

        if (action == ActionType.PORTFOLIO_EXPOSURE) {
            return AgentPlan.run(action, null, null);
        }

        if (action == null && symbols.isEmpty() && !pronoun) {
            return AgentPlan.unsupported(
                    "I can look up a quote, overview, sector, country exposure, overlap, news, a move, statements, history, or sentiment. Name the symbol."
            );
        }

        if (action == null) {
            String named = first(symbols, prior, pronoun);
            if (named == null) {
                return AgentPlan.clarify("Which symbol should I use?");
            }
            return AgentPlan.clarify(
                    "What should I look up for " + named
                            + "? Quote, sector, country exposure, news, or why it moved."
            );
        }

        if (needsTwo(action)) {
            return twoSymbols(action, symbols, prior, pronoun);
        }

        String symbol = first(symbols, prior, pronoun);
        if (symbol == null) {
            return AgentPlan.clarify("Which symbol? There is no earlier symbol to use.");
        }
        return AgentPlan.run(action, symbol, null);
    }

    private static AgentPlan twoSymbols(
            ActionType action,
            List<String> symbols,
            String prior,
            boolean pronoun) {

        if (symbols.size() >= 2) {
            return AgentPlan.run(action, symbols.get(0), symbols.get(1));
        }
        if (symbols.size() == 1 && prior != null && !prior.equals(symbols.get(0))) {
            return AgentPlan.run(action, prior, symbols.get(0));
        }
        if (pronoun && prior != null && symbols.isEmpty()) {
            return AgentPlan.clarify(
                    "Name the other symbol to " + label(action) + " with " + prior + "."
            );
        }
        return AgentPlan.clarify("Name two symbols to " + label(action) + ".");
    }

    private static boolean needsTwo(ActionType action) {
        return action == ActionType.SECURITY_COMPARISON || action == ActionType.ETF_OVERLAP;
    }

    private static String first(List<String> symbols, String prior, boolean pronoun) {
        if (!symbols.isEmpty()) {
            return symbols.getFirst();
        }
        if (pronoun) {
            return prior;
        }
        return prior;
    }

    private static boolean refersToPrevious(String upper) {
        return upper.contains(" ITS ")
                || upper.startsWith("ITS ")
                || upper.contains(" IT ")
                || upper.startsWith("IT ")
                || upper.contains(" THAT ")
                || upper.contains(" THIS ")
                || upper.contains(" THE STOCK")
                || upper.contains(" THE ETF")
                || upper.contains(" THE FUND")
                || upper.contains(" SAME ");
    }

    private static ActionType intent(String upper) {
        String padded = " " + upper + " ";
        if (contains(padded, "PORTFOLIO EXPOSURE", "MY PORTFOLIO", "PORTFOLIO SECTOR")) {
            return ActionType.PORTFOLIO_EXPOSURE;
        }
        if (contains(padded, "GEOGRAPHIC", "GEOGRAPHY", "COUNTRY EXPOSURE", "COUNTRY WEIGHT")) {
            return ActionType.ETF_GEOGRAPHIC_EXPOSURE;
        }
        if (contains(padded, "SECTOR")) {
            return ActionType.ETF_SECTOR_EXPOSURE;
        }
        if (contains(padded, "OVERLAP")) {
            return ActionType.ETF_OVERLAP;
        }
        if (contains(padded, "COMPARE", "VERSUS", " VS ")) {
            return ActionType.SECURITY_COMPARISON;
        }
        if (contains(padded, "WHY") || has(upper, "MOVED") || has(upper, "MOVE") || has(upper, "MOVEMENT")) {
            return ActionType.MARKET_MOVEMENT;
        }
        if (contains(padded, "SENTIMENT")) {
            return ActionType.NEWS_SENTIMENT;
        }
        if (contains(padded, "STATEMENT", "FINANCIALS", "INCOME", "BALANCE SHEET")) {
            return ActionType.FINANCIAL_STATEMENTS;
        }
        if (contains(padded, "HISTORY", " RSI ", "DAILY PRICE")) {
            return ActionType.PRICE_HISTORY;
        }
        if (contains(padded, "NEWS", "HEADLINE")) {
            return ActionType.FINANCIAL_NEWS;
        }
        if (contains(padded, "OVERVIEW", "DESCRIBE", "PROFILE", "WHAT IS")) {
            return ActionType.SECURITY_OVERVIEW;
        }
        if (contains(padded, "QUOTE", "PRICE", "TRADING AT")) {
            return ActionType.MARKET_QUOTE;
        }
        return null;
    }

    private static boolean has(String upper, String word) {
        return Pattern.compile("\\b" + Pattern.quote(word) + "\\b").matcher(upper).find();
    }

    private static boolean contains(String padded, String... phrases) {
        for (String phrase : phrases) {
            if (padded.contains(phrase)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> symbols(String upper) {
        List<String> found = new ArrayList<>();
        for (String token : upper.split("[^A-Z0-9.]+")) {
            if (token.isBlank() || STOP.contains(token) || !TICKER.matcher(token).matches()) {
                continue;
            }
            if (!found.contains(token)) {
                found.add(token);
            }
        }
        return found;
    }

    private static boolean isInjection(String upper) {
        return upper.contains("IGNORE PREVIOUS")
                || upper.contains("DISREGARD")
                || upper.contains("SYSTEM PROMPT")
                || upper.contains("DROP TABLE")
                || upper.contains("DELETE FROM")
                || upper.contains("SELECT *")
                || upper.contains("<SCRIPT")
                || upper.contains("EXECUTE ")
                || upper.contains("TOOL_CALL");
    }

    private static String label(ActionType action) {
        return switch (action) {
            case SECURITY_COMPARISON -> "compare";
            case ETF_OVERLAP -> "measure overlap";
            default -> "run that";
        };
    }
}
