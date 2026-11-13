package com.personal_ai.backend.service;

import com.personal_ai.backend.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserService {

    User signup(String email, String displayName, String plainPassword);

    User createUser(String email, String displayName, String passwordHash);

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);

    List<User> findAll();

    User updateUser(UUID id, String displayName, Boolean isActive);

    void deleteUser(UUID id);

    void verifyEmail(UUID id);
}
