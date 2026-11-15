package com.personal_ai.backend.controller;

import com.personal_ai.backend.model.ArchiveConversation;
import com.personal_ai.backend.model.ArchiveMessage;
import com.personal_ai.backend.service.archive.ArchiveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/archives")
@Tag(name = "Archive", description = "Archived conversation history for future model training")
public class ArchiveController {

    private final ArchiveService archiveService;

    public ArchiveController(ArchiveService archiveService) {
        this.archiveService = archiveService;
    }

    @GetMapping
    @Operation(summary = "List archived conversations", description = "Get all archived conversations for the current user")
    @ApiResponse(responseCode = "200", description = "List of archived conversations")
    public ResponseEntity<List<ArchiveConversation>> getArchivedConversations(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String userEmail = userDetails.getUsername();
        List<ArchiveConversation> archives = archiveService.getArchivedConversationsByUserEmail(userEmail);
        return ResponseEntity.ok(archives);
    }

    @GetMapping("/{archiveConversationId}/messages")
    @Operation(summary = "Get archived messages", description = "Get all messages for an archived conversation")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of archived messages"),
            @ApiResponse(responseCode = "404", description = "Archived conversation not found")
    })
    public ResponseEntity<List<ArchiveMessage>> getArchivedMessages(
            @Parameter(description = "UUID of the archived conversation")
            @PathVariable UUID archiveConversationId
    ) {
        List<ArchiveMessage> messages = archiveService.getArchivedMessages(archiveConversationId);
        return ResponseEntity.ok(messages);
    }

    @DeleteMapping("/{archiveConversationId}")
    @Operation(summary = "Delete archived conversation", description = "Permanently delete an archived conversation and its messages")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Archived conversation deleted"),
            @ApiResponse(responseCode = "404", description = "Archived conversation not found")
    })
    public ResponseEntity<?> deleteArchivedConversation(
            @Parameter(description = "UUID of the archived conversation to delete")
            @PathVariable UUID archiveConversationId
    ) {
        archiveService.deleteArchivedConversation(archiveConversationId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Archived conversation deleted successfully"
        ));
    }
}
