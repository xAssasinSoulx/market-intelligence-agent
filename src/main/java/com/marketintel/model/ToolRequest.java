package com.marketintel.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ToolRequest {

    private final long userId;
    private final ActionType action;
    private final Map<String, String> arguments;

    public ToolRequest(
            long userId,
            ActionType action,
            Map<String, String> arguments) {

        this.userId = userId;
        this.action = Objects.requireNonNull(action);
        this.arguments = new HashMap<>(
                Objects.requireNonNull(arguments)
        );
    }

    public long getUserId() {
        return userId;
    }

    public ActionType getAction() {
        return action;
    }

    public Map<String, String> getArguments() {
        return Collections.unmodifiableMap(
                arguments
        );
    }

    public String getArgument(String key) {
        return arguments.get(key);
    }
}