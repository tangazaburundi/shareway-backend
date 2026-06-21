package com.shareway.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Résumé d'une conversation (liste des boîtes de réception).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationSummaryResponse {

    /** ID de l'autre participant */
    private String participantId;
    private String participantFirstName;
    private String participantLastName;
    private String participantAvatarUrl;
    private boolean participantVerified;

    /** Dernier message de la conversation */
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private boolean lastMessageFromMe;

    /** Nombre de messages non lus */
    private long unreadCount;
}
