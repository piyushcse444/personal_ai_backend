package com.personal_ai.backend.service.auth;

import java.util.Map;
import java.util.UUID;

public interface AuthService {

	Map<String, Object> login(String email, String plainPassword);

	Map<String, Object> refresh(String refreshToken);

	void logout(String refreshToken);

	void logoutAllByUserId(UUID userId);
}
