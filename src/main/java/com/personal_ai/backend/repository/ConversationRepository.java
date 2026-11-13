package com.personal_ai.backend.repository;

import com.personal_ai.backend.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

	@Query("SELECT DISTINCT c FROM Conversation c LEFT JOIN FETCH c.model WHERE c.user.id = :userId ORDER BY c.updatedAt DESC")
	List<Conversation> findByUserIdOrderByUpdatedAtDesc(@Param("userId") UUID userId);

	List<Conversation> findByUserId(UUID userId);

	@Query("SELECT DISTINCT c FROM Conversation c LEFT JOIN FETCH c.model m LEFT JOIN FETCH m.provider p LEFT JOIN FETCH c.providerConfig pc LEFT JOIN FETCH c.provider WHERE c.user.id = :userId AND c.id = :id")
	Optional<Conversation> findByUserIdAndId(@Param("userId") UUID userId, @Param("id") UUID id);

	List<Conversation> findByUserIdAndProviderId(UUID userId, UUID providerId);

	@Query("SELECT DISTINCT c FROM Conversation c LEFT JOIN FETCH c.model WHERE c.user.id = :userId AND c.title LIKE %:search% ORDER BY c.updatedAt DESC")
	List<Conversation> findByUserIdAndTitleContaining(@Param("userId") UUID userId, @Param("search") String search);
}
