package com.marketintel.persistence;

import com.marketintel.model.Watchlist;

public interface WatchlistRepository {

    Watchlist loadForUser(long userId);

    void saveForUser(
            long userId,
            Watchlist watchlist
    );
}