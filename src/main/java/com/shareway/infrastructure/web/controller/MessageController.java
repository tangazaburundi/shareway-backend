/*
package com.shareway.infrastructure.web.controller;

import com.shareway.application.dto.request.SendMessageRequest;
import com.shareway.application.dto.response.ApiResponse;
import com.shareway.application.dto.response.ConversationSummaryResponse;
import com.shareway.application.dto.response.MessageResponse;
import com.shareway.application.dto.response.PageResponse;
import com.shareway.application.usecase.MessageUseCase;
import com.shareway.infrastructure.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

*/
/**
 * Routes messagerie :
 * <p>
 * POST   /messages                          → envoyer un message
 * GET    /messages/conversations            → liste de toutes mes conversations
 * GET    /messages/conversation/{userId}    → messages avec un utilisateur précis
 * GET    /messages/unread/count             → badge non lus
 * POST   /messages/{id}/flag               → signaler un message
 * DELETE /messages/{id}                    → supprimer mon message
 * <p>
 * Règle : n'importe quel utilisateur peut initier une conversation avec
 * n'importe quel autre (conducteur → passager, passager → conducteur, etc.).
 *//*

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
@Tag(name = "Messages", description = "Messagerie libre entre utilisateurs")
@SecurityRequirement(name = "bearerAuth")
public class MessageController {

    private final MessageUseCase messageUseCase;

    // ─────────────────────────────────────────────────────────────────────
    // POST /messages
    // Envoyer un message à n'importe quel utilisateur
    // ─────────────────────────────────────────────────────────────────────
    @PostMapping
    @Operation(
            summary = "Envoyer un message",
            description = "N'importe quel utilisateur connecté peut écrire à n'importe quel autre. " +
                    "Body : { receiverId, content }"
    )
    public ResponseEntity<ApiResponse<MessageResponse>> send(
            @Valid @RequestBody SendMessageRequest req) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        messageUseCase.send(req, SecurityUtils.currentUserId()),
                        "Message sent"));
    }

    // ─────────────────────────────────────────────────────────────────────
    // GET /messages/conversations
    // Liste de toutes mes conversations (1 entrée = dernier message par contact)
    // ─────────────────────────────────────────────────────────────────────
    @GetMapping("/conversations")
    @Operation(
            summary = "Liste de mes conversations",
            description = "Retourne un résumé de chaque conversation : dernier message, " +
                    "nombre de non lus, infos de l'autre participant."
    )
    public ResponseEntity<ApiResponse<List<ConversationSummaryResponse>>> conversations() {
        return ResponseEntity.ok(ApiResponse.ok(
                messageUseCase.getConversations(SecurityUtils.currentUserId())));
    }

    // ─────────────────────────────────────────────────────────────────────
    // GET /messages/conversation/{userId}
    // Historique complet de la conversation avec un utilisateur
    // ─────────────────────────────────────────────────────────────────────
    @GetMapping("/conversation/{userId}")
    @Operation(
            summary = "Conversation avec un utilisateur",
            description = "Retourne tous les messages échangés (dans les deux sens). " +
                    "Marque automatiquement les messages reçus comme lus."
    )
    public ResponseEntity<ApiResponse<PageResponse<MessageResponse>>> getConversation(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                messageUseCase.getConversation(
                        SecurityUtils.currentUserId(), userId, page, size)));
    }

    // ─────────────────────────────────────────────────────────────────────
    // GET /messages/unread/count
    // Nombre total de messages non lus (pour le badge dans l'UI)
    // ─────────────────────────────────────────────────────────────────────
    @GetMapping("/unread/count")
    @Operation(summary = "Nombre de messages non lus")
    public ResponseEntity<ApiResponse<Long>> unreadCount() {
        return ResponseEntity.ok(ApiResponse.ok(
                messageUseCase.countUnread(SecurityUtils.currentUserId())));
    }

    // ─────────────────────────────────────────────────────────────────────
    // POST /messages/{id}/flag
    // Signaler un message inapproprié
    // ─────────────────────────────────────────────────────────────────────
    @PostMapping("/{id}/flag")
    @Operation(
            summary = "Signaler un message",
            description = "Body : { reason: string }"
    )
    public ResponseEntity<ApiResponse<Void>> flag(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        messageUseCase.flagMessage(id, body.getOrDefault("reason", ""), SecurityUtils.currentUserId());
        return ResponseEntity.ok(ApiResponse.noContent("Message reported"));
    }

    // ─────────────────────────────────────────────────────────────────────
    // DELETE /messages/{id}
    // Soft-delete d'un message (uniquement l'expéditeur peut le faire)
    // ─────────────────────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Supprimer mon message",
            description = "Soft delete. Seul l'expéditeur peut supprimer son propre message."
    )
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        messageUseCase.deleteMessage(id, SecurityUtils.currentUserId());
        return ResponseEntity.ok(ApiResponse.noContent("Message deleted"));
    }


    @PatchMapping("/read/{otherId}")
    public ResponseEntity<Void> markAsRead(@PathVariable String otherId) {
        messageUseCase.markAsRead(otherId, SecurityUtils.currentUserId());
        return ResponseEntity.noContent().build();
    }

}
*/


package com.shareway.infrastructure.web.controller;

import com.shareway.application.dto.request.SendMessageRequest;
import com.shareway.application.dto.response.ApiResponse;
import com.shareway.application.dto.response.ConversationSummaryResponse;
import com.shareway.application.dto.response.MessageResponse;
import com.shareway.application.dto.response.PageResponse;
import com.shareway.application.usecase.MessageUseCase;
import com.shareway.infrastructure.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * ORDRE CRITIQUE : routes statiques AVANT /{id}
 * <p>
 * POST   /messages                          → envoyer
 * GET    /messages/conversations            → liste conversations
 * GET    /messages/conversation/{userId}    → messages avec un user   ← statique AVANT /{id}
 * GET    /messages/unread/count             → badge non lus            ← statique AVANT /{id}
 * PATCH  /messages/mark-read/{messageId}    → marquer comme lu         ← statique AVANT /{id}
 * POST   /messages/{id}/flag               → signaler
 * DELETE /messages/{id}                    → supprimer
 */
@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
@Tag(name = "Messages", description = "Messagerie entre utilisateurs")
@SecurityRequirement(name = "bearerAuth")
public class MessageController {

    private final MessageUseCase messageUseCase;

    // ════════════════════════════════════════════════════════════════
    // Routes sans {id}
    // ════════════════════════════════════════════════════════════════

    @PostMapping
    @Operation(summary = "Envoyer un message à n'importe quel utilisateur")
    public ResponseEntity<ApiResponse<MessageResponse>> send(
            @Valid @RequestBody SendMessageRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        messageUseCase.send(req, SecurityUtils.currentUserId()), "Message envoyé"));
    }

    @GetMapping("/conversations")
    @Operation(summary = "Toutes mes conversations (dernier message par contact)")
    public ResponseEntity<ApiResponse<List<ConversationSummaryResponse>>> conversations() {
        return ResponseEntity.ok(ApiResponse.ok(
                messageUseCase.getConversations(SecurityUtils.currentUserId())));
    }

    // ════════════════════════════════════════════════════════════════
    // Routes statiques — AVANT /{id} pour éviter le conflit
    // ════════════════════════════════════════════════════════════════

    /**
     * GET /messages/conversation/{userId}
     * Déclaré AVANT /{id} — sinon Spring interprète "conversation" comme un UUID.
     */
    @GetMapping("/conversation/{userId}")
    @Operation(
            summary = "Messages échangés avec un utilisateur",
            description = "Marque automatiquement les messages reçus comme lus."
    )
    public ResponseEntity<ApiResponse<PageResponse<MessageResponse>>> getConversation(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                messageUseCase.getConversation(SecurityUtils.currentUserId(), userId, page, size)));
    }

    /**
     * GET /messages/unread/count
     * Déclaré AVANT /{id} — sinon Spring interprète "unread" comme un UUID.
     */
    @GetMapping("/unread/count")
    @Operation(summary = "Nombre de messages non lus (badge)")
    public ResponseEntity<ApiResponse<Long>> unreadCount() {
        return ResponseEntity.ok(ApiResponse.ok(
                messageUseCase.countUnread(SecurityUtils.currentUserId())));
    }

    /**
     * PATCH /messages/mark-read/{messageId}
     * Renommé "mark-read" (tiret) au lieu de "read" pour éviter toute ambiguïté
     * avec un éventuel GET /messages/read/{id}.
     */
    @PatchMapping("/mark-read/{messageId}")
    @Operation(summary = "Marquer un message spécifique comme lu")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable String messageId) {
        messageUseCase.markAsRead(messageId, SecurityUtils.currentUserId());
        return ResponseEntity.ok(ApiResponse.noContent("Message marqué comme lu"));
    }

    // ════════════════════════════════════════════════════════════════
    // Routes avec /{id} — toujours en dernier
    // ════════════════════════════════════════════════════════════════

    @PostMapping("/{id}/flag")
    @Operation(summary = "Signaler un message")
    public ResponseEntity<ApiResponse<Void>> flag(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        messageUseCase.flagMessage(id, body.getOrDefault("reason", ""), SecurityUtils.currentUserId());
        return ResponseEntity.ok(ApiResponse.noContent("Message signalé"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer mon message (soft delete)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        messageUseCase.deleteMessage(id, SecurityUtils.currentUserId());
        return ResponseEntity.ok(ApiResponse.noContent("Message supprimé"));
    }
}

