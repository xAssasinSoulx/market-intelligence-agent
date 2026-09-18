package com.marketintel.auth;

import com.marketintel.model.User;
import com.marketintel.persistence.UserRepository;

public class AuthService {

    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_USERNAME_LENGTH = 30;
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public AuthService(
            UserRepository userRepository,
            PasswordHasher passwordHasher) {

        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    public User register(
            String username,
            String password) {

        String normalizedUsername =
                validateAndNormalizeUsername(username);

        validatePassword(password);

        if (userRepository.existsByUsername(
                normalizedUsername)) {

            throw new IllegalArgumentException(
                    "Username is already registered."
            );
        }

        String passwordHash =
                passwordHasher.hash(password);

        User user = new User(
                normalizedUsername,
                passwordHash
        );

        return userRepository.save(user);
    }

    public User authenticate(
            String username,
            String password) {

        if (username == null || password == null) {
            throw new SecurityException(
                    "Invalid username or password."
            );
        }

        String normalizedUsername =
                username.trim();

        User user = userRepository
                .findByUsername(normalizedUsername)
                .orElseThrow(
                        () -> new SecurityException(
                                "Invalid username or password."
                        )
                );

        if (!passwordHasher.verify(
                password,
                user.getPasswordHash())) {

            throw new SecurityException(
                    "Invalid username or password."
            );
        }

        return user;
    }

    private String validateAndNormalizeUsername(
            String username) {

        if (username == null) {
            throw new IllegalArgumentException(
                    "Username cannot be null."
            );
        }

        String normalized = username.trim();

        if (normalized.length() < MIN_USERNAME_LENGTH
                || normalized.length() > MAX_USERNAME_LENGTH) {

            throw new IllegalArgumentException(
                    "Username must contain between "
                            + MIN_USERNAME_LENGTH
                            + " and "
                            + MAX_USERNAME_LENGTH
                            + " characters."
            );
        }

        if (!normalized.matches("[A-Za-z0-9._-]+")) {
            throw new IllegalArgumentException(
                    "Username contains invalid characters."
            );
        }

        return normalized;
    }

    private void validatePassword(String password) {

        if (password == null
                || password.length() < MIN_PASSWORD_LENGTH) {

            throw new IllegalArgumentException(
                    "Password must contain at least "
                            + MIN_PASSWORD_LENGTH
                            + " characters."
            );
        }
    }
}