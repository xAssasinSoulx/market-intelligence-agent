package com.marketintel.app;

import com.marketintel.model.MarketMovementContext;

/**
 * Evidence gathered for a movement question, plus the optional model
 * explanation. Evidence is retained when the model is missing or fails.
 */
public final class MovementReport {

    private final String failureMessage;
    private final MarketMovementContext context;
    private final String explanation;
    private final String explanationNote;

    private MovementReport(
            String failureMessage,
            MarketMovementContext context,
            String explanation,
            String explanationNote) {

        this.failureMessage = failureMessage;
        this.context = context;
        this.explanation = explanation;
        this.explanationNote = explanationNote;
    }

    public static MovementReport failure(String message) {
        return new MovementReport(message, null, null, null);
    }

    public static MovementReport evidenceOnly(
            MarketMovementContext context,
            String note) {

        return new MovementReport(null, context, null, note);
    }

    public static MovementReport explained(
            MarketMovementContext context,
            String explanation) {

        return new MovementReport(null, context, explanation, null);
    }

    public static MovementReport explanationFailed(
            MarketMovementContext context,
            String note) {

        return new MovementReport(null, context, null, note);
    }

    public boolean hasEvidence() {
        return context != null;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public MarketMovementContext getContext() {
        return context;
    }

    public String getExplanation() {
        return explanation;
    }

    public String getExplanationNote() {
        return explanationNote;
    }
}
