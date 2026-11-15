package com.personal_ai.backend.controller;

import com.personal_ai.backend.dto.RefreshRequest;
import com.personal_ai.backend.service.auth.AuthService;
import com.personal_ai.backend.service.auth.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Token refresh and session management APIs")
public class AuthController {

	private final AuthService authService;
	private final JwtService jwtService;

	public AuthController(AuthService authService, JwtService jwtService) {
		this.authService = authService;
		this.jwtService = jwtService;
	}

	@PostMapping("/refresh")
	@Operation(summary = "Refresh access token", description = "Generate a new access token using a valid refresh token")
	public ResponseEntity<Map<String, Object>> refresh(@Valid @RequestBody RefreshRequest request) {
		try {
			Map<String, Object> result = authService.refresh(request.getRefreshToken());
			return ResponseEntity.ok(result);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of(
					"success", false,
					"message", e.getMessage()
			));
		}
	}

	@PostMapping("/logout")
	@Operation(summary = "Logout", description = "Invalidate a refresh token and log out the current session")
	public ResponseEntity<Map<String, Object>> logout(@Valid @RequestBody RefreshRequest request) {
		try {
			authService.logout(request.getRefreshToken());
			return ResponseEntity.ok(Map.of(
					"success", true,
					"message", "Logged out successfully"
			));
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of(
					"success", false,
					"message", e.getMessage()
			));
		}
	}

	@PostMapping("/logout-all")
	@Operation(summary = "Logout all sessions", description = "Revoke all active sessions for the authenticated user")
	public ResponseEntity<Map<String, Object>> logoutAll(HttpServletRequest httpRequest) {
		try {
			String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
			UUID userId = UUID.fromString(userIdStr);
			authService.logoutAllByUserId(userId);
			return ResponseEntity.ok(Map.of(
					"success", true,
					"message", "All sessions revoked"
			));
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of(
					"success", false,
					"message", e.getMessage()
			));
		}
	}
}
