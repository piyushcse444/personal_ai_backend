package com.personal_ai.backend.service.impl;

import com.personal_ai.backend.model.User;
import com.personal_ai.backend.repository.UserRepository;
import com.personal_ai.backend.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User signup(String email, String displayName, String plainPassword) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email already in use: " + email);
        }

        String passwordHash = passwordEncoder.encode(plainPassword);

        User user = User.builder()
                .email(email.trim().toLowerCase())
                .displayName(displayName.trim())
                .passwordHash(passwordHash)
                .build();

        return userRepository.save(user);
    }

    @Override
    public User createUser(String email, String displayName, String passwordHash) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email already in use: " + email);
        }

        User user = User.builder()
                .email(email.trim().toLowerCase())
                .displayName(displayName.trim())
                .passwordHash(passwordHash)
                .build();

        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email.trim().toLowerCase());
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public User updateUser(UUID id, String displayName, Boolean isActive) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));

        if (displayName != null) {
            user.setDisplayName(displayName.trim());
        }
        if (isActive != null) {
            user.setIsActive(isActive);
        }

        return userRepository.save(user);
    }

    @Override
    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("User not found: " + id);
        }
        userRepository.deleteById(id);
    }

    @Override
    public void verifyEmail(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));

        user.setEmailVerifiedAt(OffsetDateTime.now());
        userRepository.save(user);
    }
}
