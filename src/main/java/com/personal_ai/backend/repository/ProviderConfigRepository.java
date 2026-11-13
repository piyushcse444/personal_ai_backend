package com.personal_ai.backend.repository;

import com.personal_ai.backend.model.ProviderConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProviderConfigRepository extends JpaRepository<ProviderConfig, UUID> {

	@Query("SELECT DISTINCT pc FROM ProviderConfig pc LEFT JOIN FETCH pc.user LEFT JOIN FETCH pc.provider WHERE pc.id = :id")
	Optional<ProviderConfig> findById(UUID id);

	@Query("SELECT DISTINCT pc FROM ProviderConfig pc LEFT JOIN FETCH pc.provider WHERE pc.user.id = :userId AND pc.isActive = true")
	List<ProviderConfig> findByUserIdAndIsActiveTrue(UUID userId);

	@Query("SELECT DISTINCT pc FROM ProviderConfig pc LEFT JOIN FETCH pc.provider WHERE pc.user.id = :userId AND pc.provider.id = :providerId AND pc.isActive = true")
	List<ProviderConfig> findByUserIdAndProviderIdAndIsActiveTrue(UUID userId, UUID providerId);

	@Query("SELECT DISTINCT pc FROM ProviderConfig pc LEFT JOIN FETCH pc.provider WHERE pc.user.id = :userId")
	List<ProviderConfig> findByUserId(UUID userId);

	@Query("SELECT DISTINCT pc FROM ProviderConfig pc LEFT JOIN FETCH pc.user LEFT JOIN FETCH pc.provider WHERE pc.user.id = :userId AND pc.id = :id")
	Optional<ProviderConfig> findByUserIdAndId(UUID userId, UUID id);
}
