package com.marketintel.agent;

import com.marketintel.model.ActionType;

/**
 * Prompts for the follow-up model. The model may only name an allowlisted
 * action. It does not receive instructions to call other systems.
 */
public final class PromptBuilder {

    public String planPrompt(String question, String previousSymbol) {
        String prior = previousSymbol == null || previousSymbol.isBlank()
                ? "none"
                : previousSymbol.trim();
        return """
                You route a market-research question to one tool.
                Reply with one JSON object and no other text.

                Allowed action values:
                MARKET_QUOTE, SECURITY_OVERVIEW, ETF_SECTOR_EXPOSURE,
                ETF_GEOGRAPHIC_EXPOSURE, SECURITY_COMPARISON, ETF_OVERLAP,
                FINANCIAL_NEWS, MARKET_MOVEMENT, FINANCIAL_STATEMENTS,
                PRICE_HISTORY, NEWS_SENTIMENT, PORTFOLIO_EXPOSURE,
                CLARIFY, UNSUPPORTED

                Rules:
                - Use only those action names.
                - symbol and secondSymbol are tickers such as NVDA or QQQ, or null.
                - If the user says it, its, or that, set symbol to the previous symbol.
                - If you cannot tell which symbol, use CLARIFY and a short message.
                - PORTFOLIO_EXPOSURE does not need a symbol.
                - SECURITY_COMPARISON and ETF_OVERLAP need two symbols.
                - Do not invent a ticker.

                Previous symbol: %s
                Question: %s

                Shape:
                {"action":"MARKET_QUOTE","symbol":"NVDA","secondSymbol":null,"message":null}
                """.formatted(prior, clip(question, 500));
    }

    public String answerPrompt(String question, AgentPlan plan, String evidence) {
        return """
                Answer the question in two or three plain sentences.
                Use only the evidence below. Do not invent prices, dates, or causes.
                Do not give a buy or sell recommendation.
                If the evidence is an error, say what failed.

                Question: %s
                Action: %s
                Symbol: %s

                Evidence:
                %s
                """.formatted(
                clip(question, 500),
                plan.action(),
                plan.symbol() == null ? "" : plan.symbol(),
                clip(evidence, 3500)
        );
    }

    private static String clip(String text, int limit) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.length() <= limit) {
            return trimmed;
        }
        return trimmed.substring(0, limit);
    }
}
