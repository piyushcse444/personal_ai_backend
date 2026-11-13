package com.personal_ai.backend.service;

import com.personal_ai.backend.model.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {

	@Value("${app.jwt.secret}")
	private String secret;

	@Value("${app.jwt.expiration-ms}")
	private long expirationMs;

	@Value("${app.jwt.refresh-expiration-ms}")
	private long refreshExpirationMs;

	private SecretKey getSigningKey() {
		byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
		return Keys.hmacShaKeyFor(keyBytes);
	}

	public String generateToken(User user) {
		Date now = new Date();
		Date expiry = new Date(now.getTime() + expirationMs);

		Map<String, Object> claims = new HashMap<>();
		claims.put("email", user.getEmail());
		claims.put("displayName", user.getDisplayName());

		return Jwts.builder()
				.claims()
				.subject(user.getId().toString())
				.add(claims)
				.issuedAt(now)
				.expiration(expiry)
				.and()
				.signWith(getSigningKey(), Jwts.SIG.HS256)
				.compact();
	}

	public String generateRefreshToken(User user) {
		UUID refreshToken = UUID.randomUUID();
		return refreshToken.toString();
	}

	public String getUserIdFromToken(String token) {
		return Jwts.parser()
				.verifyWith(getSigningKey())
				.build()
				.parseSignedClaims(token)
				.getPayload()
				.getSubject();
	}

	public boolean validateToken(String token) {
		try {
			Jwts.parser()
					.verifyWith(getSigningKey())
					.build()
					.parseSignedClaims(token);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
