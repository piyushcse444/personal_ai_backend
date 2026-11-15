package com.personal_ai.backend.repository;

import com.personal_ai.backend.model.ArchiveMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ArchiveMessageRepository extends JpaRepository<ArchiveMessage, UUID> {

    List<ArchiveMessage> findByArchiveConversationId(UUID archiveConversationId);

    List<ArchiveMessage> findByArchiveConversationIdOrderByArchivedAtAsc(UUID archiveConversationId);

    void deleteByArchiveConversationId(UUID archiveConversationId);
}
