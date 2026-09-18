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
                        commandParser
                );

        userInterface.start();
    }
}