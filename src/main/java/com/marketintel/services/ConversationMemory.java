package com.marketintel.services;

import com.marketintel.model.ConversationTurn;
import com.marketintel.persistence.ConversationRepository;

import java.util.List;

/**
 * Per-user conversation memory. A later question can see the last symbol
 * this user named. Another user's rows are never read.
 */
public final class ConversationMemory {

    private final ConversationRepository repository;

    public ConversationMemory(ConversationRepository repository) {
        if (repository == null) {
            throw new IllegalArgumentException("Conversation repository cannot be null.");
        }
        this.repository = repository;
    }

    public void remember(long userId, String role, String body, String symbol, String action) {
        repository.append(new ConversationTurn(userId, role, body, symbol, action));
    }

    public List<ConversationTurn> recent(long userId) {
        return repository.recent(userId, 12);
    }

    public String lastSymbol(long userId) {
        List<ConversationTurn> turns = repository.recent(userId, 20);
        for (int index = turns.size() - 1; index >= 0; index--) {
            String symbol = turns.get(index).symbol();
            if (symbol != null && !symbol.isBlank()) {
                return symbol;
            }
        }
        return null;
    }
}
