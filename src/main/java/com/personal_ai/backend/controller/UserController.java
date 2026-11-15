package com.personal_ai.backend.controller;

import com.personal_ai.backend.dto.LoginRequest;
import com.personal_ai.backend.dto.SignupRequest;
import com.personal_ai.backend.dto.UpdateUserRequest;
import com.personal_ai.backend.model.User;
import com.personal_ai.backend.service.auth.AuthService;
import com.personal_ai.backend.service.auth.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@Tag(name = "User", description = "User registration, login, and profile management APIs")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    public UserController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @PostMapping("/api/auth/signup")
    @Operation(summary = "Sign up", description = "Register a new user account with email, display name, and password")
    public ResponseEntity<Map<String, Object>> signup(@Valid @RequestBody SignupRequest request) {
        try {
            User user = userService.signup(request.getEmail(), request.getDisplayName(), request.getPassword());

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "message", "User registered successfully",
                    "userId", user.getId(),
                    "email", user.getEmail(),
                    "displayName", user.getDisplayName()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/api/auth/login")
    @Operation(summary = "Login", description = "Authenticate a user and receive JWT access and refresh tokens")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        try {
            Map<String, Object> result = authService.login(request.getEmail(), request.getPassword());
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/api/users/me")
    @Operation(summary = "Get current user", description = "Retrieve the authenticated user's profile information")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        try {
            String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            UUID userId = UUID.fromString(userIdStr);

            User user = userService.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            return ResponseEntity.ok(Map.of(
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "displayName", user.getDisplayName()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PatchMapping("/api/users/me")
    @Operation(summary = "Update current user", description = "Update the authenticated user's profile information")
    public ResponseEntity<Map<String, Object>> updateCurrentUser(@Valid @RequestBody UpdateUserRequest request) {
        try {
            String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            UUID userId = UUID.fromString(userIdStr);

            User user = userService.updateUser(userId, request.getDisplayName(), null);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Profile updated successfully",
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "displayName", user.getDisplayName()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @DeleteMapping("/api/users/me")
    @Operation(summary = "Delete current user", description = "Permanently delete the authenticated user's account")
    public ResponseEntity<Map<String, Object>> deleteCurrentUser() {
        try {
            String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            UUID userId = UUID.fromString(userIdStr);

            userService.deleteUser(userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Account deleted successfully"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}
