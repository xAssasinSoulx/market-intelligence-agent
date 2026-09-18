package com.marketintel.auth;

public interface PasswordHasher {

    String hash(String password);

    boolean verify(String password, String passwordHash);
}