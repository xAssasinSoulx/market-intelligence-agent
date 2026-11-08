package com.marketintel.ui;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CommandParser {

    private static final Set<String> NOT_A_SYMBOL = Set.of(
            "did", "does", "is", "was", "has", "have", "the", "a", "an",
            "it", "its", "that", "this"
    );

    private static final Set<String> VERBS = Set.of(
            "help",
            "whoami",
            "portfolio",
            "watchlist",
            "quote",
            "overview",
            "compare",
            "news",
            "statements",
            "statement",
            "history",
            "sentiment",
            "why",
            "ask",
            "sector",
            "geography",
            "geo",
            "overlap",
            "logout",
            "exit"
    );

    public boolean isCommand(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }
        String trimmed = stripOptionalSlash(input.trim());
        if (trimmed.isEmpty()) {
            return false;
        }
        String[] parts = trimmed.split("\\s+");
        String verb = parts[0].toLowerCase(Locale.ROOT);
        if (!VERBS.contains(verb)) {
            return input.trim().startsWith("/");
        }
        if (verb.equals("why") && !isSymbolWhy(parts)) {
            return false;
        }
        return true;
    }

    public ParsedCommand parse(String input) {

        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException(
                    "Command cannot be empty."
            );
        }

        String trimmed = stripOptionalSlash(input.trim());

        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(
                    "Command cannot be empty."
            );
        }

        String[] parts =
                trimmed.split("\\s+");

        String command =
                parts[0].toLowerCase(Locale.ROOT);

        List<String> arguments =
                Arrays.stream(parts)
                        .skip(1)
                        .toList();

        return new ParsedCommand(
                command,
                arguments
        );
    }

    private static String stripOptionalSlash(String trimmed) {
        if (trimmed.startsWith("/")) {
            return trimmed.substring(1).trim();
        }
        return trimmed;
    }

    /**
     * {@code why MSFT} is the symbol command. A longer sentence, such as
     * {@code why did microsoft fall}, is a question for the follow-up planner.
     */
    private static boolean isSymbolWhy(String[] parts) {
        if (parts.length == 1) {
            return true;
        }
        return parts.length == 2
                && !NOT_A_SYMBOL.contains(parts[1].toLowerCase(Locale.ROOT))
                && parts[1].matches("[A-Za-z][A-Za-z0-9.]{0,14}");
    }
}
