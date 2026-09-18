package com.marketintel;

import com.marketintel.auth.AuthService;
import com.marketintel.auth.BCryptPasswordHasher;
import com.marketintel.auth.PasswordHasher;
import com.marketintel.auth.SessionManager;
import com.marketintel.persistence.DatabaseManager;
import com.marketintel.persistence.SQLiteUserRepository;
import com.marketintel.persistence.UserRepository;

public class Main {

    private static final String DATABASE_URL =
            "jdbc:sqlite:data/market-intelligence.db";

    public static void main(String[] args) {

        DatabaseManager databaseManager =
                new DatabaseManager(DATABASE_URL);

        databaseManager.initialize();

        UserRepository userRepository =
                new SQLiteUserRepository(databaseManager);

        PasswordHasher passwordHasher =
                new BCryptPasswordHasher();

        AuthService authService =
                new AuthService(
                        userRepository,
                        passwordHasher
                );

        SessionManager sessionManager =
                new SessionManager();

        System.out.println(
                "Market Intelligence Agent"
        );

        System.out.println(
                "Database initialized successfully."
        );
    }
}