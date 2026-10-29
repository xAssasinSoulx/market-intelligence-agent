package com.marketintel.app;

import com.marketintel.auth.AuthService;
import com.marketintel.auth.SessionManager;
import com.marketintel.model.AssetType;
import com.marketintel.model.Portfolio;
import com.marketintel.model.User;
import com.marketintel.model.Watchlist;
import com.marketintel.services.PortfolioService;
import com.marketintel.services.WatchlistService;

import java.math.BigDecimal;

/**
 * Session-scoped access to account, portfolio, and watchlist services.
 * Callers cannot choose another user's id; every operation uses the
 * authenticated session or fails.
 */
public final class SessionWorkspace {

    private final AuthService authService;
    private final SessionManager sessionManager;
    private final PortfolioService portfolioService;
    private final WatchlistService watchlistService;

    public SessionWorkspace(
            AuthService authService,
            SessionManager sessionManager,
            PortfolioService portfolioService,
            WatchlistService watchlistService) {

        this.authService = authService;
        this.sessionManager = sessionManager;
        this.portfolioService = portfolioService;
        this.watchlistService = watchlistService;
    }

    public User register(String username, String password) {
        return authService.register(username, password);
    }

    public User login(String username, String password) {
        User user = authService.authenticate(username, password);
        sessionManager.startSession(user);
        return user;
    }

    public void logout() {
        sessionManager.endSession();
    }

    public boolean isAuthenticated() {
        return sessionManager.isAuthenticated();
    }

    public User currentUser() {
        return sessionManager.getCurrentUser();
    }

    public Portfolio portfolio() {
        return portfolioService.getPortfolio(currentUserId());
    }

    public void addPosition(
            String symbol,
            BigDecimal quantity,
            AssetType assetType) {

        portfolioService.addPosition(
                currentUserId(),
                symbol,
                quantity,
                assetType
        );
    }

    public boolean removePosition(String symbol) {
        return portfolioService.removePosition(currentUserId(), symbol);
    }

    public Watchlist watchlist() {
        return watchlistService.getWatchlist(currentUserId());
    }

    public void addToWatchlist(String symbol) {
        watchlistService.addSymbol(currentUserId(), symbol);
    }

    public boolean removeFromWatchlist(String symbol) {
        return watchlistService.removeSymbol(currentUserId(), symbol);
    }

    private long currentUserId() {
        return sessionManager.getCurrentUserId();
    }
}
