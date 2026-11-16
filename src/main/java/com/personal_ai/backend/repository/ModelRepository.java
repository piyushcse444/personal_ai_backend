package com.personal_ai.backend.repository;

import com.personal_ai.backend.model.Model;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ModelRepository extends JpaRepository<Model, UUID> {

	@Query("SELECT DISTINCT m FROM Model m LEFT JOIN FETCH m.provider WHERE m.id = :id")
	Optional<Model> findById(UUID id);

	@Query("SELECT DISTINCT m FROM Model m LEFT JOIN FETCH m.provider WHERE m.provider.id = :providerId AND m.isActive = true")
	List<Model> findByProviderIdAndIsActiveTrue(UUID providerId);

	@Query("SELECT DISTINCT m FROM Model m LEFT JOIN FETCH m.provider WHERE m.isActive = true")
	List<Model> findByIsActiveTrue();

	@Query("SELECT DISTINCT m FROM Model m LEFT JOIN FETCH m.provider WHERE m.provider.id = :providerId")
	List<Model> findByProviderId(UUID providerId);

	@Query("SELECT DISTINCT m FROM Model m LEFT JOIN FETCH m.provider WHERE m.modelType = :modelType AND m.isActive = true")
	List<Model> findByModelTypeAndIsActiveTrue(Model.ModelType modelType);

	@Query("SELECT DISTINCT m FROM Model m LEFT JOIN FETCH m.provider WHERE m.modelType = :modelType")
	List<Model> findByModelType(Model.ModelType modelType);
}
