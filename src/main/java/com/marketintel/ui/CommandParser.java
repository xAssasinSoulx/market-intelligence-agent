package com.marketintel.ui;

import java.util.Arrays;
import java.util.List;

public class CommandParser {

    public boolean isCommand(String input) {
        return input != null
                && input.trim().startsWith("/");
    }

    public ParsedCommand parse(String input) {

        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException(
                    "Command cannot be empty."
            );
        }

        String trimmed = input.trim();

        if (!trimmed.startsWith("/")) {
            throw new IllegalArgumentException(
                    "Commands must begin with '/'."
            );
        }

        String withoutSlash =
                trimmed.substring(1).trim();

        if (withoutSlash.isEmpty()) {
            throw new IllegalArgumentException(
                    "Command cannot be empty."
            );
        }

        String[] parts =
                withoutSlash.split("\\s+");

        String command =
                parts[0].toLowerCase();

        List<String> arguments =
                Arrays.stream(parts)
                        .skip(1)
                        .toList();

        return new ParsedCommand(
                command,
                arguments
        );
    }
}