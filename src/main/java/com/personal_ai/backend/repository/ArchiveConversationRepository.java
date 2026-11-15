package com.personal_ai.backend.repository;

import com.personal_ai.backend.model.ArchiveConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ArchiveConversationRepository extends JpaRepository<ArchiveConversation, UUID> {

    List<ArchiveConversation> findByUserEmail(String userEmail);

    List<ArchiveConversation> findByUserEmailOrderByArchivedAtDesc(String userEmail);
}
