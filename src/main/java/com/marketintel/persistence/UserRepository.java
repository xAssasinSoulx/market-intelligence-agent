package com.marketintel.persistence;

import com.marketintel.model.User;

import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(long userId);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);
}