package com.personal_ai.backend.service.impl;

import com.personal_ai.backend.model.AuthSession;
import com.personal_ai.backend.model.User;
import com.personal_ai.backend.service.AuthService;
import com.personal_ai.backend.service.AuthSessionService;
import com.personal_ai.backend.service.JwtService;
import com.personal_ai.backend.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

	private final UserService userService;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final AuthSessionService authSessionService;

	public AuthServiceImpl(UserService userService, PasswordEncoder passwordEncoder, JwtService jwtService, AuthSessionService authSessionService) {
		this.userService = userService;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.authSessionService = authSessionService;
	}

	@Override
	public Map<String, Object> login(String email, String plainPassword) {
		Optional<User> userOpt = userService.findByEmail(email);

		if (userOpt.isEmpty()) {
			throw new IllegalArgumentException("Invalid email or password");
		}

		User user = userOpt.get();

		if (!passwordEncoder.matches(plainPassword, user.getPasswordHash())) {
			throw new IllegalArgumentException("Invalid email or password");
		}

		String accessToken = jwtService.generateToken(user);
		String refreshToken = jwtService.generateRefreshToken(user);
		byte[] refreshTokenHash = refreshToken.getBytes();

		authSessionService.createSession(
				user.getId(),
				refreshTokenHash,
				null,
				null
		);

		return Map.of(
				"success", true,
				"message", "Login successful",
				"token", accessToken,
				"refreshToken", refreshToken
		);
	}

	@Override
	public Map<String, Object> refresh(String refreshToken) {
		byte[] tokenBytes = refreshToken.getBytes();

		Optional<AuthSession> sessionOpt = authSessionService.findByRefreshTokenHash(tokenBytes);
		if (sessionOpt.isEmpty()) {
			throw new IllegalArgumentException("Invalid refresh token");
		}

		AuthSession session = sessionOpt.get();
		if (session.getRevokedAt() != null) {
			throw new IllegalArgumentException("Refresh token has been revoked");
		}
		if (session.getExpiresAt().isBefore(OffsetDateTime.now())) {
			throw new IllegalArgumentException("Refresh token has expired");
		}

		User user = session.getUser();

		String newRefreshToken = jwtService.generateRefreshToken(user);
		byte[] newRefreshTokenHash = newRefreshToken.getBytes();
		OffsetDateTime now = OffsetDateTime.now();

		session.setRevokedAt(now);
		session.setLastUsedAt(now);

		authSessionService.createSession(
				user.getId(),
				newRefreshTokenHash,
				session.getUserAgent(),
				session.getIpAddress() != null ? session.getIpAddress().getHostAddress() : null
		);

		String newAccessToken = jwtService.generateToken(user);

		return Map.of(
				"success", true,
				"token", newAccessToken,
				"refreshToken", newRefreshToken
		);
	}

	@Override
	public void logout(String refreshToken) {
		byte[] tokenBytes = refreshToken.getBytes();

		Optional<AuthSession> sessionOpt = authSessionService.findByRefreshTokenHash(tokenBytes);
		if (sessionOpt.isPresent()) {
			authSessionService.revokeSession(sessionOpt.get().getId());
		}
	}

	@Override
	public void logoutAllByUserId(UUID userId) {
		authSessionService.revokeAllSessionsByUserId(userId);
	}
}
