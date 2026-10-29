package com.marketintel.agent;

import com.marketintel.app.MovementReport;
import com.marketintel.model.ActionType;
import com.marketintel.model.MarketMovementContext;
import com.marketintel.model.ToolRequest;
import com.marketintel.model.ToolResult;
import com.marketintel.providers.LLMException;
import com.marketintel.providers.LLMProvider;
import com.marketintel.services.ConversationMemory;
import com.marketintel.services.MarketExplanationService;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Follow-up pipeline: read the last symbol, plan an allowlisted action,
 * run that tool, then store both sides of the exchange for this user only.
 */
public final class AgentController {

    private final ConversationMemory memory;
    private final RequestPlanner planner;
    private final ToolManager tools;
    private final ResponseComposer composer;
    private final MarketExplanationService explanations;
    private final LLMProvider languageModel;
    private final PromptBuilder prompts = new PromptBuilder();

    public AgentController(
            ConversationMemory memory,
            RequestPlanner planner,
            ToolManager tools,
            ResponseComposer composer,
            MarketExplanationService explanations) {

        this(memory, planner, tools, composer, explanations, null);
    }

    public AgentController(
            ConversationMemory memory,
            RequestPlanner planner,
            ToolManager tools,
            ResponseComposer composer,
            MarketExplanationService explanations,
            LLMProvider languageModel) {

        if (memory == null || planner == null || tools == null || composer == null) {
            throw new IllegalArgumentException("Follow-up dependencies cannot be null.");
        }
        this.memory = memory;
        this.planner = planner;
        this.tools = tools;
        this.composer = composer;
        this.explanations = explanations;
        this.languageModel = languageModel;
    }

    public String handle(long userId, String input) {
        String previous = memory.lastSymbol(userId);
        AgentPlan plan = planner.createPlan(input, previous);
        String symbol = plan.symbol();
        String actionName = plan.action() == null ? null : plan.action().name();
        memory.remember(userId, "user", input, symbol, actionName);

        String reply = switch (plan.kind()) {
            case CLARIFY, UNSUPPORTED -> composer.composeMessage(plan.message());
            case RUN -> run(userId, input, plan);
        };
        memory.remember(userId, "assistant", reply, symbol, actionName);
        return reply;
    }

    /**
     * Records a symbol looked up from a button or a slash command, so a
     * later question can say "it" and mean that symbol.
     */
    public void noteLookup(long userId, String symbol, ActionType action) {
        if (symbol == null || symbol.isBlank()) {
            return;
        }
        String normalized = symbol.trim().toUpperCase(Locale.ROOT);
        memory.remember(
                userId,
                "user",
                normalized,
                normalized,
                action == null ? null : action.name()
        );
    }

    private String run(long userId, String question, AgentPlan plan) {
        if (plan.action() == ActionType.MARKET_MOVEMENT) {
            return movement(userId, plan);
        }
        ToolResult result = tools.execute(new ToolRequest(userId, plan.action(), arguments(plan)));
        String evidence = composer.compose(plan, result);
        if (!result.isSuccess()
                || languageModel == null
                || plan.action() == ActionType.PORTFOLIO_EXPOSURE) {
            return evidence;
        }
        try {
            String spoken = languageModel.generateBrief(prompts.answerPrompt(question, plan, evidence));
            if (spoken == null || spoken.isBlank()) {
                return evidence;
            }
            return spoken.trim() + "\n\n" + evidence;
        } catch (RuntimeException exception) {
            return evidence;
        }
    }

    private String movement(long userId, AgentPlan plan) {
        ToolResult evidence = tools.execute(new ToolRequest(
                userId,
                ActionType.MARKET_MOVEMENT,
                Map.of("symbol", plan.symbol())
        ));
        if (!evidence.isSuccess()) {
            return composer.compose(plan, evidence);
        }
        if (!(evidence.getData() instanceof MarketMovementContext context)) {
            return composer.composeMessage("Unexpected market movement response.");
        }
        if (explanations == null) {
            return composer.composeMovement(
                    plan,
                    MovementReport.evidenceOnly(
                            context,
                            "Gemini is not configured. The quote and news evidence above are still available."
                    )
            );
        }
        try {
            return composer.composeMovement(
                    plan,
                    MovementReport.explained(context, explanations.explain(context))
            );
        } catch (LLMException exception) {
            return composer.composeMovement(
                    plan,
                    MovementReport.explanationFailed(context, exception.getMessage())
            );
        }
    }

    private static Map<String, String> arguments(AgentPlan plan) {
        Map<String, String> arguments = new HashMap<>();
        if (plan.action() == ActionType.SECURITY_COMPARISON
                || plan.action() == ActionType.ETF_OVERLAP) {
            arguments.put("firstSymbol", plan.symbol());
            arguments.put("secondSymbol", plan.secondSymbol());
            return arguments;
        }
        if (plan.symbol() != null) {
            arguments.put("symbol", plan.symbol());
        }
        return arguments;
    }
}
