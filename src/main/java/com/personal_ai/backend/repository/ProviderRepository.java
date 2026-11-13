package com.personal_ai.backend.repository;

import com.personal_ai.backend.model.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProviderRepository extends JpaRepository<Provider, UUID> {

	@Query("SELECT DISTINCT p FROM Provider p LEFT JOIN FETCH p.models WHERE p.id = :id")
	Optional<Provider> findById(UUID id);

    Optional<Provider> findByCode(String code);

	@Query("SELECT DISTINCT p FROM Provider p LEFT JOIN FETCH p.models WHERE p.isActive = true")
    List<Provider> findByIsActiveTrue();

    boolean existsByCode(String code);
}
