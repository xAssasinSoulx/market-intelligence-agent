package com.marketintel.app;

import com.marketintel.agent.ToolManager;
import com.marketintel.auth.SessionManager;
import com.marketintel.model.ActionType;
import com.marketintel.model.MarketMovementContext;
import com.marketintel.model.ToolRequest;
import com.marketintel.model.ToolResult;
import com.marketintel.providers.LLMException;
import com.marketintel.services.MarketExplanationService;

import java.util.Map;

/**
 * Runs registered tools for the signed-in user. The user id always comes
 * from the active session, and an in-flight request is rejected if that
 * user changes before the call finishes.
 */
public final class ResearchGateway {

    private final SessionManager sessionManager;
    private final ToolManager toolManager;
    private final MarketExplanationService marketExplanationService;

    public ResearchGateway(
            SessionManager sessionManager,
            ToolManager toolManager,
            MarketExplanationService marketExplanationService) {

        this.sessionManager = sessionManager;
        this.toolManager = toolManager;
        this.marketExplanationService = marketExplanationService;
    }

    public boolean isAvailable(ActionType action) {
        return toolManager.findTool(action).isPresent();
    }

    public long currentUserId() {
        return sessionManager.getCurrentUserId();
    }

    public ToolResult execute(
            long expectedUserId,
            ActionType action,
            Map<String, String> arguments) {

        if (action == null) {
            return ToolResult.failure("An action is required.");
        }

        if (!sessionManager.isAuthenticated()
                || sessionManager.getCurrentUserId() != expectedUserId) {

            return ToolResult.failure(
                    "Sign in again before using this feature."
            );
        }

        if (!isAvailable(action)) {
            return ToolResult.failure(unavailableMessage(action));
        }

        Map<String, String> safeArguments =
                arguments == null ? Map.of() : arguments;

        return toolManager.execute(
                new ToolRequest(expectedUserId, action, safeArguments)
        );
    }

    public MovementReport explainMovement(
            long expectedUserId,
            String symbol) {

        ToolResult evidence = execute(
                expectedUserId,
                ActionType.MARKET_MOVEMENT,
                Map.of("symbol", symbol == null ? "" : symbol)
        );

        if (!evidence.isSuccess()) {
            return MovementReport.failure(evidence.getErrorMessage());
        }

        if (!(evidence.getData() instanceof MarketMovementContext context)) {
            return MovementReport.failure(
                    "Unexpected market movement response."
            );
        }

        if (marketExplanationService == null) {
            return MovementReport.evidenceOnly(
                    context,
                    "Gemini is not configured. The quote and news evidence above are still available."
            );
        }

        try {
            String explanation = marketExplanationService.explain(context);
            return MovementReport.explained(context, explanation);
        } catch (LLMException exception) {
            return MovementReport.explanationFailed(
                    context,
                    exception.getMessage()
            );
        }
    }

    public static String unavailableMessage(ActionType action) {
        return switch (action) {
            case ETF_GEOGRAPHIC_EXPOSURE ->
                    "Geographic ETF exposure is unavailable. "
                            + "Set FINNHUB_API_KEY and restart. "
                            + "Country weights are the provider's reported fund exposure, "
                            + "not a guessed allocation.";
            case PORTFOLIO_EXPOSURE ->
                    "Portfolio exposure is unavailable. "
                            + "Set FINNHUB_API_KEY and restart.";
            case MARKET_QUOTE, SECURITY_OVERVIEW ->
                    "Quotes and security overviews are unavailable. "
                            + "Set FINNHUB_API_KEY and restart.";
            case SECURITY_COMPARISON ->
                    "Security comparison is unavailable. "
                            + "Set FINNHUB_API_KEY and restart.";
            case ETF_SECTOR_EXPOSURE, ETF_OVERLAP ->
                    "ETF data is unavailable. "
                            + "Set ALPHA_VANTAGE_API_KEY and restart.";
            case FINANCIAL_NEWS ->
                    "Financial news is unavailable. "
                            + "Set MARKETAUX_API_TOKEN and restart.";
            case MARKET_MOVEMENT ->
                    "Movement analysis is unavailable. It requires "
                            + "FINNHUB_API_KEY and MARKETAUX_API_TOKEN.";
            default -> "No tool is registered for " + action + ".";
        };
    }
}
