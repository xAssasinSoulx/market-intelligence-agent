package com.marketintel;

import com.marketintel.auth.AuthService;
import com.marketintel.auth.BCryptPasswordHasher;
import com.marketintel.auth.PasswordHasher;
import com.marketintel.auth.SessionManager;

import com.marketintel.persistence.DatabaseManager;
import com.marketintel.persistence.SQLiteUserRepository;
import com.marketintel.persistence.UserRepository;

import com.marketintel.ui.CommandParser;
import com.marketintel.ui.TerminalUI;
import com.marketintel.ui.UserInterface;

import com.marketintel.persistence.PortfolioRepository;
import com.marketintel.persistence.SQLitePortfolioRepository;
import com.marketintel.persistence.WatchlistRepository;
import com.marketintel.persistence.SQLiteWatchlistRepository;

import com.marketintel.services.PortfolioService;
import com.marketintel.services.WatchlistService;


public class Main {

    private static final String DATABASE_URL =
            "jdbc:sqlite:data/market-intelligence.db";

    public static void main(String[] args) {

        // ----------------------------------------------------
        // Persistence
        // ----------------------------------------------------

        DatabaseManager databaseManager =
                new DatabaseManager(DATABASE_URL);

        databaseManager.initialize();

        UserRepository userRepository =
                new SQLiteUserRepository(
                        databaseManager
                );
        PortfolioRepository portfolioRepository =
                new SQLitePortfolioRepository(
                        databaseManager
                );

        WatchlistRepository watchlistRepository =
                new SQLiteWatchlistRepository(
                        databaseManager
                );

        PortfolioService portfolioService =
                new PortfolioService(
                        portfolioRepository
                );

        WatchlistService watchlistService =
                new WatchlistService(
                        watchlistRepository
                );

        // ----------------------------------------------------
        // Authentication
        // ----------------------------------------------------

        PasswordHasher passwordHasher =
                new BCryptPasswordHasher();

        AuthService authService =
                new AuthService(
                        userRepository,
                        passwordHasher
                );

        SessionManager sessionManager =
                new SessionManager();

        // ----------------------------------------------------
        // User Interface
        // ----------------------------------------------------

        CommandParser commandParser =
                new CommandParser();

        UserInterface userInterface =
                new TerminalUI(
                        authService,
                        sessionManager,
                        commandParser,
                        portfolioService,
                        watchlistService
                );

        userInterface.start();
    }
}