package com.marketintel.tools;

import com.marketintel.model.ActionType;
import com.marketintel.model.ToolRequest;
import com.marketintel.model.ToolResult;
import com.marketintel.model.ValidationResult;
import com.marketintel.providers.FinancialStatementProvider;
import com.marketintel.providers.MarketDataException;
import com.marketintel.providers.PriceHistoryProvider;
import com.marketintel.providers.SentimentProvider;
import com.marketintel.services.RequestValidator;

/**
 * Statements, price history, and the sentiment score. Each action is
 * registered only when that provider was wired, so a missing key does
 * not look like an empty report.
 */
public final class MarketInsightTool implements AgentTool {

    private final FinancialStatementProvider statements;
    private final PriceHistoryProvider history;
    private final SentimentProvider sentiment;
    private final RequestValidator validator;

    public MarketInsightTool(
            FinancialStatementProvider statements,
            PriceHistoryProvider history,
            SentimentProvider sentiment,
            RequestValidator validator) {

        if (statements == null && history == null && sentiment == null) {
            throw new IllegalArgumentException("At least one insight provider is required.");
        }
        if (validator == null) {
            throw new IllegalArgumentException("Request validator cannot be null.");
        }
        this.statements = statements;
        this.history = history;
        this.sentiment = sentiment;
        this.validator = validator;
    }

    @Override
    public String getName() {
        return "market-insight";
    }

    @Override
    public boolean supports(ActionType action) {
        return switch (action) {
            case FINANCIAL_STATEMENTS -> statements != null;
            case PRICE_HISTORY -> history != null;
            case NEWS_SENTIMENT -> sentiment != null;
            default -> false;
        };
    }

    @Override
    public ToolResult execute(ToolRequest request) {
        ValidationResult requestValidation = validator.validateToolRequest(request);
        if (!requestValidation.isValid()) {
            return ToolResult.failure(requestValidation.getErrorMessage());
        }
        if (!supports(request.getAction())) {
            return ToolResult.failure("Unsupported insight action: " + request.getAction());
        }

        String symbol = request.getArgument("symbol");
        ValidationResult symbolValidation = validator.validateSymbol(symbol);
        if (!symbolValidation.isValid()) {
            return ToolResult.failure(symbolValidation.getErrorMessage());
        }
        String normalized = symbol.trim().toUpperCase();

        try {
            return switch (request.getAction()) {
                case FINANCIAL_STATEMENTS ->
                        ToolResult.success(statements.getFinancialStatements(normalized));
                case PRICE_HISTORY ->
                        ToolResult.success(history.getPriceHistory(normalized));
                case NEWS_SENTIMENT ->
                        ToolResult.success(sentiment.getSentiment(normalized));
                default -> ToolResult.failure("Unsupported insight action.");
            };
        } catch (MarketDataException exception) {
            return ToolResult.failure(exception.getMessage());
        }
    }
}
