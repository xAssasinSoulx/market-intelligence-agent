package com.marketintel.agent;

import com.marketintel.model.ActionType;

/**
 * A validated next step. The planner never asks a model to name a tool.
 */
public record AgentPlan(
        Kind kind,
        ActionType action,
        String symbol,
        String secondSymbol,
        String message) {

    public enum Kind {
        RUN,
        CLARIFY,
        UNSUPPORTED
    }

    public static AgentPlan run(ActionType action, String symbol, String secondSymbol) {
        return new AgentPlan(Kind.RUN, action, symbol, secondSymbol, null);
    }

    public static AgentPlan clarify(String message) {
        return new AgentPlan(Kind.CLARIFY, null, null, null, message);
    }

    public static AgentPlan unsupported(String message) {
        return new AgentPlan(Kind.UNSUPPORTED, null, null, null, message);
    }
}
