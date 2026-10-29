package com.marketintel.model;

/**
 * One stored line of a signed-in user's conversation.
 * The symbol is the referent resolved for that line, when there was one.
 */
public record ConversationTurn(
        long userId,
        String role,
        String body,
        String symbol,
        String action) {
}
