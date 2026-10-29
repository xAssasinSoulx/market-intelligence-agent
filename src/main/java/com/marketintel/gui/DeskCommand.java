package com.marketintel.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Parses the desk command line. The commands match the terminal verbs
 * so a person can move between the window and the CLI without learning
 * a second language.
 */
public final class DeskCommand {

    public enum Action {
        EMPTY,
        HELP,
        HOME,
        RESEARCH,
        PORTFOLIO,
        WATCHLIST,
        EXPLAIN,
        LOGOUT,
        QUOTE,
        OVERVIEW,
        COMPARE,
        SECTOR,
        OVERLAP,
        NEWS,
        WHY,
        PORTFOLIO_ADD,
        PORTFOLIO_REMOVE,
        WATCHLIST_ADD,
        WATCHLIST_REMOVE,
        GEOGRAPHY,
        EXPOSURE,
        INVALID
    }

    private final Action action;
    private final List<String> arguments;
    private final String message;

    private DeskCommand(Action action, List<String> arguments, String message) {
        this.action = action;
        this.arguments = List.copyOf(arguments);
        this.message = message;
    }

    public Action action() {
        return action;
    }

    public List<String> arguments() {
        return arguments;
    }

    public String message() {
        return message;
    }

    public String argument(int index) {
        if (index < 0 || index >= arguments.size()) {
            return "";
        }
        return arguments.get(index);
    }

    public static DeskCommand parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return new DeskCommand(
                    Action.EMPTY,
                    List.of(),
                    "Enter a command. HELP lists them."
            );
        }

        String text = raw.trim();
        if (text.startsWith("/")) {
            text = text.substring(1).trim();
        }
        if (text.isEmpty()) {
            return new DeskCommand(
                    Action.EMPTY,
                    List.of(),
                    "Enter a command. HELP lists them."
            );
        }

        String[] parts = text.split("\\s+");
        String verb = parts[0].toUpperCase(Locale.ROOT);
        List<String> args = new ArrayList<>();
        for (int index = 1; index < parts.length; index++) {
            args.add(parts[index].toUpperCase(Locale.ROOT));
        }

        return switch (verb) {
            case "HELP", "?" -> help();
            case "HOME", "MENU", "1" -> navigate(Action.HOME);
            case "RESEARCH", "DESK", "2" -> navigate(Action.RESEARCH);
            case "3" -> navigate(Action.PORTFOLIO);
            case "4" -> navigate(Action.WATCHLIST);
            case "5" -> navigate(Action.EXPLAIN);
            case "LOGOUT", "LOGOFF" -> navigate(Action.LOGOUT);
            case "QUOTE", "Q" -> needs(Action.QUOTE, args, 1, "Usage: QUOTE SYMBOL");
            case "OVERVIEW", "OVW" -> needs(Action.OVERVIEW, args, 1, "Usage: OVERVIEW SYMBOL");
            case "DES" -> des(args);
            case "COMPARE", "CMP" -> needs(Action.COMPARE, args, 2, "Usage: COMPARE SYMBOL SYMBOL");
            case "SECTOR", "SEC" -> needs(Action.SECTOR, args, 1, "Usage: SECTOR ETF");
            case "OVERLAP", "OVL" -> needs(Action.OVERLAP, args, 2, "Usage: OVERLAP ETF ETF");
            case "NEWS", "N" -> needs(Action.NEWS, args, 1, "Usage: NEWS SYMBOL");
            case "WHY", "EXPLAIN" -> why(args);
            case "GEO", "GEOGRAPHY" -> unavailable(
                    Action.GEOGRAPHY,
                    "Geographic exposure is not implemented. SECTOR ETF is available."
            );
            case "EXPOSURE" -> navigate(Action.EXPOSURE);
            case "PORT", "PORTFOLIO" -> portfolio(args);
            case "WATCH", "MON", "WATCHLIST" -> watchlist(args);
            default -> new DeskCommand(
                    Action.INVALID,
                    args,
                    "Unknown command. Type HELP."
            );
        };
    }

    public static String helpText() {
        return """
                COMMANDS
                --------
                1 HOME                         Session monitor
                2 DES                          Research screen
                3 PORT                         Portfolio
                4 MON                          Watchlist
                5 WHY                          Movement explanation
                QUOTE SYMBOL                   Last price and change
                OVERVIEW SYMBOL                Security description
                COMPARE SYMBOL SYMBOL          Change difference
                SECTOR ETF                     Sector weights
                OVERLAP ETF ETF                Shared holdings
                NEWS SYMBOL                    Recent articles
                WHY SYMBOL                     Evidence, then Gemini
                EXPOSURE                       Portfolio sector weights
                PORT ADD SYMBOL QTY TYPE       TYPE is STOCK, ETF, or OTHER
                PORT REMOVE SYMBOL
                WATCH ADD SYMBOL
                WATCH REMOVE SYMBOL
                HELP
                LOGOUT

                GEO is recognized and reports that country exposure is not implemented.
                A leading slash is accepted, as in /quote NVDA.
                """;
    }

    private static DeskCommand help() {
        return new DeskCommand(Action.HELP, List.of(), helpText());
    }

    private static DeskCommand navigate(Action action) {
        return new DeskCommand(action, List.of(), null);
    }

    private static DeskCommand unavailable(Action action, String message) {
        return new DeskCommand(action, List.of(), message);
    }

    private static DeskCommand needs(
            Action action,
            List<String> args,
            int count,
            String usage) {

        if (args.size() < count) {
            return new DeskCommand(Action.INVALID, args, usage);
        }
        return new DeskCommand(action, args, null);
    }

    private static DeskCommand des(List<String> args) {
        if (args.isEmpty()) {
            return navigate(Action.RESEARCH);
        }
        return needs(Action.OVERVIEW, args, 1, "Usage: DES SYMBOL");
    }

    private static DeskCommand why(List<String> args) {
        if (args.isEmpty()) {
            return navigate(Action.EXPLAIN);
        }
        return needs(Action.WHY, args, 1, "Usage: WHY SYMBOL");
    }

    private static DeskCommand portfolio(List<String> args) {
        if (args.isEmpty()) {
            return navigate(Action.PORTFOLIO);
        }
        String operation = args.getFirst();
        List<String> rest = args.subList(1, args.size());
        return switch (operation) {
            case "ADD" -> needs(
                    Action.PORTFOLIO_ADD,
                    rest,
                    3,
                    "Usage: PORT ADD SYMBOL QTY TYPE"
            );
            case "REMOVE", "DEL" -> needs(
                    Action.PORTFOLIO_REMOVE,
                    rest,
                    1,
                    "Usage: PORT REMOVE SYMBOL"
            );
            case "EXPOSURE" -> navigate(Action.EXPOSURE);
            default -> new DeskCommand(
                    Action.INVALID,
                    args,
                    "Usage: PORT, PORT ADD SYMBOL QTY TYPE, or PORT REMOVE SYMBOL"
            );
        };
    }

    private static DeskCommand watchlist(List<String> args) {
        if (args.isEmpty()) {
            return navigate(Action.WATCHLIST);
        }
        String operation = args.getFirst();
        List<String> rest = args.subList(1, args.size());
        return switch (operation) {
            case "ADD" -> needs(Action.WATCHLIST_ADD, rest, 1, "Usage: WATCH ADD SYMBOL");
            case "REMOVE", "DEL" -> needs(
                    Action.WATCHLIST_REMOVE,
                    rest,
                    1,
                    "Usage: WATCH REMOVE SYMBOL"
            );
            default -> new DeskCommand(
                    Action.INVALID,
                    args,
                    "Usage: WATCH, WATCH ADD SYMBOL, or WATCH REMOVE SYMBOL"
            );
        };
    }
}
