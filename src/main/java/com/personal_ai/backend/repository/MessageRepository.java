package com.personal_ai.backend.repository;

import com.personal_ai.backend.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

	@Query("SELECT DISTINCT m FROM Message m LEFT JOIN FETCH m.model WHERE m.conversation.id = :conversationId ORDER BY m.createdAt ASC, m.id ASC")
	List<Message> findByConversationIdOrderByCreatedAtAsc(@Param("conversationId") UUID conversationId);

	List<Message> findByConversationId(UUID conversationId);

	void deleteByConversationId(UUID conversationId);
}
