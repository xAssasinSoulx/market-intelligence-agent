package com.marketintel.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketintel.model.ActionType;
import com.marketintel.providers.LLMProvider;

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
    private static final Set<ActionType> ALLOWED = Set.of(
            ActionType.MARKET_QUOTE,
            ActionType.SECURITY_OVERVIEW,
            ActionType.ETF_SECTOR_EXPOSURE,
            ActionType.ETF_GEOGRAPHIC_EXPOSURE,
            ActionType.SECURITY_COMPARISON,
            ActionType.ETF_OVERLAP,
            ActionType.FINANCIAL_NEWS,
            ActionType.MARKET_MOVEMENT,
            ActionType.FINANCIAL_STATEMENTS,
            ActionType.PRICE_HISTORY,
            ActionType.NEWS_SENTIMENT,
            ActionType.PORTFOLIO_EXPOSURE
    );
    private static final Set<String> STOP = Set.of(
            "A", "AN", "AND", "ABOUT", "ASK", "THE", "ITS", "IT", "THAT", "THIS",
            "WHAT", "WHY", "HOW", "DID", "DOES", "DO", "MOVE", "MOVED", "MOVEMENT",
            "SECTOR", "SECTORS", "NEWS", "QUOTE", "PRICE", "OVERVIEW", "COMPARE",
            "OVERLAP", "WITH", "VERSUS", "VS", "GEO", "COUNTRY", "EXPOSURE",
            "GEOGRAPHIC", "GEOGRAPHY", "FINANCIAL", "STATEMENT", "STATEMENTS",
            "HISTORY", "SENTIMENT", "PORTFOLIO", "DAILY", "RSI", "SHOW", "ME",
            "FOR", "OF", "ON", "TO", "PLEASE", "RECENT", "LAST", "STOCK", "ETF",
            "FUND", "SAME", "ONE", "THEM", "THEIR", "MY", "IS", "ARE", "WAS",
            "TELL", "GET", "GIVE", "LOOK", "UP", "INTO", "FROM", "THAN", "BOTH",
            "DOING", "TODAY", "GOOD", "BAD", "NOW", "JUST", "LIKE", "VERY",
            "MUCH", "SOME", "ANY", "CAN", "YOU", "YOUR", "WOULD", "COULD",
            "SHOULD", "GOING", "WELL", "RIGHT", "STILL", "ALSO", "MORE", "ELSE",
            "INFO", "DATA", "SHARES", "COMPANY", "NAME", "HIGH", "LOW", "OPEN",
            "CLOSE", "DAY", "WEEK", "YEAR", "TIME", "WHEN", "WHERE", "WHO",
            "WHICH", "THERE", "HERE", "BEEN", "HAVE", "HAS", "HAD", "WILL",
            "REALLY", "PRETTY", "KIND", "SORT", "THING", "THINGS", "HAPPEN",
            "HAPPENING", "PERFORM", "PERFORMANCE", "LOOKING", "SEEMS", "SEEM",
            "LOOKS", "WANT", "NEED", "KNOW", "THINK", "BECAUSE", "AFTER",
            "BEFORE", "DURING", "WHILE", "OVER", "UNDER", "ONLY", "EVEN",
            "BACK", "OUT", "OFF", "DOWN", "FALL", "FALLING", "FELL", "DROP",
            "DROPPED", "WHATS", "HOWS", "WHYS", "DONT", "DOESNT"
    );

    private final LLMProvider languageModel;
    private final PromptBuilder prompts;
    private final ObjectMapper mapper = new ObjectMapper();

    public RequestPlanner() {
        this(null);
    }

    public RequestPlanner(LLMProvider languageModel) {
        this.languageModel = languageModel;
        this.prompts = new PromptBuilder();
    }

    public AgentPlan createPlan(String input, String previousSymbol) {
        AgentPlan keywords = keywordPlan(input, previousSymbol);
        if (languageModel == null || input == null || input.isBlank()) {
            return keywords;
        }
        if (keywords.kind() == AgentPlan.Kind.UNSUPPORTED
                && keywords.message() != null
                && keywords.message().contains("not run as a command")) {
            return keywords;
        }
        try {
            AgentPlan interpreted = interpret(
                    languageModel.generate(prompts.planPrompt(input, previousSymbol)),
                    previousSymbol
            );
            return interpreted == null ? keywords : interpreted;
        } catch (RuntimeException exception) {
            return keywords;
        }
    }

    public AgentPlan keywordPlan(String input, String previousSymbol) {
        if (input == null || input.isBlank()) {
            return AgentPlan.clarify("Ask a question, such as why NVDA moved or what its sector exposure is.");
        }
        String text = input.trim();
        if (text.length() > 500) {
            return AgentPlan.unsupported("That question is too long. Ask about one symbol and one action.");
        }
        String upper = text.toUpperCase(Locale.ROOT)
                .replace("'", "")
                .replace("’", "");
        upper = applyNames(upper);
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

    private AgentPlan interpret(String raw, String previousSymbol) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        JsonNode node;
        try {
            node = mapper.readTree(raw.substring(start, end + 1));
        } catch (Exception exception) {
            return null;
        }
        String actionName = node.path("action").asText("").trim().toUpperCase(Locale.ROOT);
        String message = clip(node.path("message").asText(""));
        if ("CLARIFY".equals(actionName)) {
            return AgentPlan.clarify(message.isBlank() ? "Which symbol should I use?" : message);
        }
        if ("UNSUPPORTED".equals(actionName)) {
            return AgentPlan.unsupported(message.isBlank()
                    ? "I can only answer market questions about a symbol."
                    : message);
        }
        ActionType action;
        try {
            action = ActionType.valueOf(actionName);
        } catch (IllegalArgumentException exception) {
            return null;
        }
        if (!ALLOWED.contains(action)) {
            return null;
        }
        String prior = previousSymbol == null ? null : previousSymbol.trim().toUpperCase(Locale.ROOT);
        String symbol = resolveSymbol(node.path("symbol").asText(""), prior);
        String second = resolveSymbol(node.path("secondSymbol").asText(""), null);
        if (action == ActionType.PORTFOLIO_EXPOSURE) {
            return AgentPlan.run(action, null, null);
        }
        if (needsTwo(action)) {
            if (symbol == null || second == null) {
                return AgentPlan.clarify("Name two symbols to " + label(action) + ".");
            }
            return AgentPlan.run(action, symbol, second);
        }
        if (symbol == null) {
            return AgentPlan.clarify("Which symbol? There is no earlier symbol to use.");
        }
        return AgentPlan.run(action, symbol, null);
    }

    private static String resolveSymbol(String raw, String previous) {
        if (raw == null || raw.isBlank() || raw.equalsIgnoreCase("null")) {
            return previous;
        }
        String token = raw.trim().toUpperCase(Locale.ROOT);
        if (token.equals("IT") || token.equals("ITS") || token.equals("THAT")
                || token.equals("THIS") || token.equals("PREVIOUS") || token.equals("SAME")) {
            return previous;
        }
        if (!TICKER.matcher(token).matches()) {
            return null;
        }
        return token;
    }

    private static String clip(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        return trimmed.length() <= 300 ? trimmed : trimmed.substring(0, 300);
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
        if (has(upper, "DROP") || has(upper, "DROPPED") || has(upper, "FALL")
                || has(upper, "FALLING") || has(upper, "FELL") || has(upper, "DOWN")
                || has(upper, "CRASH") || has(upper, "RALLY") || has(upper, "SURGE")
                || has(upper, "EXPLAIN")) {
            if (padded.contains("COMPANY") || padded.contains("BUSINESS") || has(upper, "DOES")) {
                return ActionType.SECURITY_OVERVIEW;
            }
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
        if (contains(padded, "OVERVIEW", "DESCRIBE", "PROFILE", "WHAT IS", "TELL ME ABOUT", "WHO IS")) {
            return ActionType.SECURITY_OVERVIEW;
        }
        if (contains(padded, "QUOTE", "PRICE", "TRADING AT")) {
            return ActionType.MARKET_QUOTE;
        }
        if (has(upper, "HOW") || has(upper, "DOING") || padded.contains("HAPPENING")
                || padded.contains("PERFORMANCE")) {
            return ActionType.MARKET_QUOTE;
        }
        if (has(upper, "EXPOSURE")) {
            return ActionType.ETF_SECTOR_EXPOSURE;
        }
        return null;
    }

    private static String applyNames(String upper) {
        String named = upper;
        for (var entry : NAMES.entrySet()) {
            named = named.replaceAll("\\b" + entry.getKey() + "\\b", entry.getValue());
        }
        return named;
    }

    private static final java.util.Map<String, String> NAMES = java.util.Map.ofEntries(
            java.util.Map.entry("NVIDIA", "NVDA"),
            java.util.Map.entry("APPLE", "AAPL"),
            java.util.Map.entry("MICROSOFT", "MSFT"),
            java.util.Map.entry("GOOGLE", "GOOGL"),
            java.util.Map.entry("ALPHABET", "GOOGL"),
            java.util.Map.entry("AMAZON", "AMZN"),
            java.util.Map.entry("TESLA", "TSLA"),
            java.util.Map.entry("FACEBOOK", "META"),
            java.util.Map.entry("NETFLIX", "NFLX"),
            java.util.Map.entry("INTEL", "INTC"),
            java.util.Map.entry("BROADCOM", "AVGO"),
            java.util.Map.entry("DISNEY", "DIS"),
            java.util.Map.entry("BOEING", "BA"),
            java.util.Map.entry("WALMART", "WMT"),
            java.util.Map.entry("JPMORGAN", "JPM")
    );

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
