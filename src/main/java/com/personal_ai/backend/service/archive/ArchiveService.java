package com.personal_ai.backend.service.archive;

import com.personal_ai.backend.model.ArchiveConversation;
import com.personal_ai.backend.model.ArchiveMessage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArchiveService {

    ArchiveConversation archiveConversation(UUID conversationId);

    List<ArchiveConversation> getArchivedConversationsByUserEmail(String userEmail);

    Optional<ArchiveConversation> getArchivedConversationById(UUID archiveConversationId);

    List<ArchiveMessage> getArchivedMessages(UUID archiveConversationId);

    void deleteArchivedConversation(UUID archiveConversationId);
}
