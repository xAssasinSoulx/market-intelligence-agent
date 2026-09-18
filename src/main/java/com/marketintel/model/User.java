package com.marketintel.model;

import java.time.LocalDateTime;

public class User {

    private final long id;
    private final String username;
    private final String passwordHash;
    private final LocalDateTime createdAt;

    public User(long id,
                String username,
                String passwordHash,
                LocalDateTime createdAt) {

        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    public User(String username, String passwordHash) {
        this(
                0,
                username,
                passwordHash,
                LocalDateTime.now()
        );
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}