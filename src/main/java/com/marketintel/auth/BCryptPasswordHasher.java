package com.marketintel.auth;

import org.mindrot.jbcrypt.BCrypt;

public class BCryptPasswordHasher implements PasswordHasher {

    private static final int WORK_FACTOR = 12;

    @Override
    public String hash(String password) {

        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException(
                    "Password cannot be null or blank."
            );
        }

        return BCrypt.hashpw(
                password,
                BCrypt.gensalt(WORK_FACTOR)
        );
    }

    @Override
    public boolean verify(
            String password,
            String passwordHash) {

        if (password == null || passwordHash == null) {
            return false;
        }

        try {
            return BCrypt.checkpw(
                    password,
                    passwordHash
            );
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}