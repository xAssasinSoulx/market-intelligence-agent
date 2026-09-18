package com.marketintel.services;

import com.marketintel.model.Watchlist;
import com.marketintel.persistence.WatchlistRepository;

public class WatchlistService {

    private final WatchlistRepository repository;

    public WatchlistService(
            WatchlistRepository repository) {

        this.repository = repository;
    }

    public Watchlist getWatchlist(long userId) {

        validateUserId(userId);

        return repository.loadForUser(userId);
    }

    public void addSymbol(
            long userId,
            String symbol) {

        validateUserId(userId);

        Watchlist watchlist =
                repository.loadForUser(userId);

        watchlist.add(symbol);

        repository.saveForUser(
                userId,
                watchlist
        );
    }

    public boolean removeSymbol(
            long userId,
            String symbol) {

        validateUserId(userId);

        Watchlist watchlist =
                repository.loadForUser(userId);

        boolean removed =
                watchlist.remove(symbol);

        if (removed) {
            repository.saveForUser(
                    userId,
                    watchlist
            );
        }

        return removed;
    }

    private void validateUserId(long userId) {

        if (userId <= 0) {
            throw new IllegalArgumentException(
                    "Invalid user ID."
            );
        }
    }
}