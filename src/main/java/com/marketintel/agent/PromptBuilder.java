package com.marketintel.agent;

import com.marketintel.model.ConversationTurn;

import java.util.List;

/**
 * Prompts for the follow-up model. The model may only name an allowlisted
 * action. It does not receive instructions to call other systems.
 */
public final class PromptBuilder {

    public String planPrompt(String question, String previousSymbol, String transcript) {
        String prior = previousSymbol == null || previousSymbol.isBlank()
                ? "none"
                : previousSymbol.trim();
        String earlier = transcript == null || transcript.isBlank() ? "none" : transcript;
        return """
                You route a market-research question to one tool, or you continue the conversation.
                Reply with one JSON object and no other text.

                Allowed action values:
                MARKET_QUOTE, SECURITY_OVERVIEW, ETF_SECTOR_EXPOSURE,
                ETF_GEOGRAPHIC_EXPOSURE, SECURITY_COMPARISON, ETF_OVERLAP,
                FINANCIAL_NEWS, MARKET_MOVEMENT, FINANCIAL_STATEMENTS,
                PRICE_HISTORY, NEWS_SENTIMENT, PORTFOLIO_EXPOSURE,
                CONVERSATION, CLARIFY, UNSUPPORTED

                Rules:
                - Use only those action names.
                - symbol and secondSymbol are tickers such as NVDA or QQQ, or null.
                - If the user says it, its, or that, set symbol to the previous symbol.
                - If you cannot tell which symbol, use CLARIFY and a short message.
                - PORTFOLIO_EXPOSURE does not need a symbol.
                - SECURITY_COMPARISON and ETF_OVERLAP need two symbols.
                - Use CONVERSATION when they greet you, ask what you can do, or ask to rephrase, shorten, or explain the previous answer. CONVERSATION does not fetch new data.
                - A question about why a price rose or fell is MARKET_MOVEMENT, not FINANCIAL_NEWS. FINANCIAL_NEWS is only for a request to list headlines.
                - Do not invent a ticker.

                Previous symbol: %s
                Earlier exchange:
                %s
                Question: %s

                Shape:
                {"action":"MARKET_QUOTE","symbol":"NVDA","secondSymbol":null,"message":null}
                """.formatted(prior, earlier, clip(question, 500));
    }

    public String answerPrompt(String question, AgentPlan plan, String evidence, String transcript) {
        String earlier = transcript == null || transcript.isBlank() ? "none" : transcript;
        return """
                Answer in four to six spoken sentences.
                Use only the evidence. Do not invent a price, a date, or a reason that is not written there.
                When the evidence includes articles, explain the up or down move from what those articles say.
                Attribute each reason to the reporting, for example "the articles attribute the move to".
                Do not say the articles fail to explain the move when they discuss demand, guidance, valuation, or a prior drop.
                A sentiment score is not itself a cause. The article text is.
                Do not give a buy or sell recommendation.
                If the evidence is an error, say what failed.

                Earlier exchange:
                %s

                Question: %s
                Action: %s
                Symbol: %s

                Evidence:
                %s
                """.formatted(
                earlier,
                clip(question, 500),
                plan.action(),
                plan.symbol() == null ? "" : plan.symbol(),
                clip(evidence, 3500)
        );
    }

    public String conversationPrompt(String question, String transcript) {
        String earlier = transcript == null || transcript.isBlank() ? "none" : transcript;
        return """
                Continue a market-research conversation in two or three spoken sentences.
                Use only the earlier exchange. Do not invent prices, dates, headlines, or causes.
                Do not give a buy or sell recommendation.
                If they greet you or ask what you can do, say you can look up a quote, news, an ETF breakdown, or why a symbol moved.
                If they ask you to rephrase or shorten, restate only what is already in the exchange, in plainer words.
                If the exchange does not contain the answer, ask which symbol and what to look up. Do not guess a number.

                Earlier exchange:
                %s

                Latest message: %s
                """.formatted(earlier, clip(question, 500));
    }

    public static String transcript(List<ConversationTurn> turns) {
        if (turns == null || turns.isEmpty()) {
            return "";
        }
        int from = Math.max(0, turns.size() - 6);
        StringBuilder builder = new StringBuilder();
        for (int index = from; index < turns.size(); index++) {
            ConversationTurn turn = turns.get(index);
            builder.append(turn.role() == null ? "note" : turn.role()).append(": ");
            if ("PORTFOLIO_EXPOSURE".equals(turn.action())) {
                builder.append("[portfolio figures omitted]");
            } else {
                builder.append(clip(turn.body(), 400));
            }
            builder.append('\n');
        }
        return builder.toString().trim();
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
