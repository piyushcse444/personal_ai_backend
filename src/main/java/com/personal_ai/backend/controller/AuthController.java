package com.personal_ai.backend.controller;

import com.personal_ai.backend.dto.RefreshRequest;
import com.personal_ai.backend.service.AuthService;
import com.personal_ai.backend.service.JwtService;
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
public class AuthController {

	private final AuthService authService;
	private final JwtService jwtService;

	public AuthController(AuthService authService, JwtService jwtService) {
		this.authService = authService;
		this.jwtService = jwtService;
	}

	@PostMapping("/refresh")
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
