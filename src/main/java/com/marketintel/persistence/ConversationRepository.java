package com.marketintel.persistence;

import com.marketintel.model.ConversationTurn;

import java.util.List;

public interface ConversationRepository {

    void append(ConversationTurn turn);

    List<ConversationTurn> recent(long userId, int limit);
}
