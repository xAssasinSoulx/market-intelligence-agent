package com.marketintel.agent;

import com.marketintel.app.MovementReport;
import com.marketintel.gui.ResultFormatter;
import com.marketintel.model.ActionType;
import com.marketintel.model.ToolResult;

/**
 * Turns a tool result into the text a person reads. It does not call a model
 * and it does not recalculate the figures.
 */
public final class ResponseComposer {

    public String compose(AgentPlan plan, ToolResult result) {
        String header = header(plan);
        String body = body(plan.action(), result);
        return header + body + "\nThis is not investment advice.\n";
    }

    public String composeMovement(AgentPlan plan, MovementReport report) {
        return header(plan) + ResultFormatter.movement(report)
                + "\nThis is not investment advice.\n";
    }

    public String composeMessage(String message) {
        if (message == null || message.isBlank()) {
            return "I could not answer that.\n";
        }
        return message.trim() + "\n";
    }

    private static String header(AgentPlan plan) {
        String symbol = plan.symbol() == null ? "" : plan.symbol();
        String second = plan.secondSymbol() == null ? "" : " " + plan.secondSymbol();
        return "FOLLOW-UP  " + symbol + second + "  ·  " + label(plan.action()) + "\n\n";
    }

    private static String body(ActionType action, ToolResult result) {
        if (action == null) {
            return ResultFormatter.unavailable("No action was selected.");
        }
        return switch (action) {
            case MARKET_QUOTE -> ResultFormatter.quote(result);
            case SECURITY_OVERVIEW -> ResultFormatter.overview(result);
            case SECURITY_COMPARISON -> ResultFormatter.comparison(result);
            case ETF_SECTOR_EXPOSURE -> ResultFormatter.sectorExposure(result);
            case ETF_GEOGRAPHIC_EXPOSURE -> ResultFormatter.geographicExposure(result);
            case ETF_OVERLAP -> ResultFormatter.overlap(result);
            case FINANCIAL_NEWS -> ResultFormatter.news(result);
            case FINANCIAL_STATEMENTS -> ResultFormatter.statements(result);
            case PRICE_HISTORY -> ResultFormatter.priceHistory(result);
            case NEWS_SENTIMENT -> ResultFormatter.sentiment(result);
            case PORTFOLIO_EXPOSURE -> ResultFormatter.portfolioExposure(result);
            case MARKET_MOVEMENT -> result.isSuccess()
                    ? ResultFormatter.unavailable("That action has no follow-up view.")
                    : ResultFormatter.unavailable(result.getErrorMessage());
            default -> ResultFormatter.unavailable("That action has no follow-up view.");
        };
    }

    private static String label(ActionType action) {
        if (action == null) {
            return "QUESTION";
        }
        return switch (action) {
            case MARKET_QUOTE -> "QUOTE";
            case SECURITY_OVERVIEW -> "OVERVIEW";
            case SECURITY_COMPARISON -> "COMPARE";
            case ETF_SECTOR_EXPOSURE -> "SECTOR";
            case ETF_GEOGRAPHIC_EXPOSURE -> "COUNTRY";
            case ETF_OVERLAP -> "OVERLAP";
            case FINANCIAL_NEWS -> "NEWS";
            case MARKET_MOVEMENT -> "WHY";
            case FINANCIAL_STATEMENTS -> "STATEMENTS";
            case PRICE_HISTORY -> "HISTORY";
            case NEWS_SENTIMENT -> "SENTIMENT";
            case PORTFOLIO_EXPOSURE -> "EXPOSURE";
            default -> action.name();
        };
    }
}
