package com.marketintel.ui;

import java.util.Collections;
import java.util.List;

public class ParsedCommand {

    private final String command;
    private final List<String> arguments;

    public ParsedCommand(String command, List<String> arguments) {
        this.command = command;
        this.arguments = List.copyOf(arguments);
    }

    public String getCommand() {
        return command;
    }

    public List<String> getArguments() {
        return Collections.unmodifiableList(arguments);
    }
}