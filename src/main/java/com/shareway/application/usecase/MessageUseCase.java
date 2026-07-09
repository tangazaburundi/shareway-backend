package com.shareway.application.usecase;

import com.shareway.application.dto.request.SendMessageRequest;
import com.shareway.application.dto.response.ConversationSummaryResponse;
import com.shareway.application.dto.response.MessageResponse;
import com.shareway.application.dto.response.PageResponse;
import com.shareway.application.port.out.AuditPort;
import com.shareway.application.port.out.NotificationPort;
import com.shareway.domain.exception.InvalidOperationException;
import com.shareway.domain.exception.UserNotFoundException;
import com.shareway.domain.model.Message;
import com.shareway.domain.model.User;
import com.shareway.domain.repository.MessageRepository;
import com.shareway.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MessageUseCase {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final NotificationPort notificationPort;
    private final AuditPort auditPort;

    // ─────────────────────────────────────────────────────────────────────
    // Envoyer un message
    // N'importe quel utilisateur peut écrire à n'importe quel autre,
    // du moment qu'ils ne s'écrivent pas à eux-mêmes.
    // ─────────────────────────────────────────────────────────────────────

    /*
    public MessageResponse send(SendMessageRequest req, String senderId) {
        if (senderId.equals(req.getReceiverId()))
            throw new InvalidOperationException("Cannot send a message to yourself");

        User sender = userRepository.findByIdAndDeletedAtIsNull(senderId)
                .orElseThrow(() -> new UserNotFoundException("Sender not found"));
        User receiver = userRepository.findByIdAndDeletedAtIsNull(req.getReceiverId())
                .orElseThrow(() -> new UserNotFoundException("Receiver not found: " + req.getReceiverId()));

        if (receiver.isBlocked())
            throw new InvalidOperationException("This user cannot receive messages");

        Message msg = Message.builder()
                .sender(sender)
                .receiver(receiver)
                .content(req.getContent())
                .build();

        messageRepository.save(msg);

        // Notification push WebSocket
        notificationPort.notifyWithLink(
                receiver.getId(),
                "MESSAGE",
                sender.getFirstName() + " " + sender.getLastName(),
                req.getContent(),
                "/messages/" + senderId
        );

        return toResponse(msg, senderId);
    }
    */

    // ── Envoyer un message ────────────────────────────────────────────────
    public MessageResponse send(SendMessageRequest req, String senderId) {
        if (senderId.equals(req.getReceiverId()))
            throw new InvalidOperationException("Impossible de s'envoyer un message à soi-même");

        User sender = userRepository.findByIdAndDeletedAtIsNull(senderId)
                .orElseThrow(() -> new UserNotFoundException("Expéditeur introuvable"));
        User receiver = userRepository.findByIdAndDeletedAtIsNull(req.getReceiverId())
                .orElseThrow(() -> new UserNotFoundException("Destinataire introuvable: " + req.getReceiverId()));

        if (receiver.isBlocked())
            throw new InvalidOperationException("Cet utilisateur ne peut pas recevoir de messages");

        Message msg = Message.builder()
                .sender(sender).receiver(receiver).content(req.getContent()).build();
        messageRepository.save(msg);

        notificationPort.notifyWithLink(
                receiver.getId(), "MESSAGE",
                sender.getFirstName() + " " + sender.getLastName(),
                req.getContent(),
                "/messages/" + senderId
        );
        return toResponse(msg);
    }


    // ─────────────────────────────────────────────────────────────────────
    // Récupérer la conversation avec un utilisateur (bidirectionnelle)
    // Route : GET /messages/conversation/{otherUserId}
    // ─────────────────────────────────────────────────────────────────────

    /*
    @Transactional(readOnly = false)
    public PageResponse<MessageResponse> getConversation(
            String currentUserId, String otherUserId, int page, int size) {

        // Vérifier que l'autre utilisateur existe
        userRepository.findByIdAndDeletedAtIsNull(otherUserId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + otherUserId));

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());

        // Marquer les messages reçus comme lus
        messageRepository.markConversationAsRead(currentUserId, otherUserId);

        return PageResponse.from(
                messageRepository
                        .findConversation(currentUserId, otherUserId, pageable)
                        .map(m -> toResponse(m, currentUserId))
        );
    }
*/


    // ── Conversation avec un utilisateur ─────────────────────────────────
    // ATTENTION : cette méthode fait un UPDATE (markAsRead) donc PAS readOnly
    @Transactional
    public PageResponse<MessageResponse> getConversation(
            String currentUserId, String otherUserId, int page, int size) {

        userRepository.findByIdAndDeletedAtIsNull(otherUserId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable: " + otherUserId));

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());

        // Marquer comme lus — DOIT être dans une transaction d'écriture
        messageRepository.markConversationAsRead(currentUserId, otherUserId);

        return PageResponse.from(
                messageRepository.findConversation(currentUserId, otherUserId, pageable)
                        .map(this::toResponse));
    }

    // ─────────────────────────────────────────────────────────────────────
    // Liste de toutes mes conversations (boîte de réception)
    // Route : GET /messages/conversations
    // ─────────────────────────────────────────────────────────────────────
    /*
    @Transactional(readOnly = true)
    public List<ConversationSummaryResponse> getConversations(String currentUserId) {
        List<Message> lastMessages = messageRepository
                .findLastMessagePerConversation(currentUserId);

        return lastMessages.stream().map(msg -> {
            // Identifier l'autre participant
            boolean iAmSender = msg.getSender().getId().equals(currentUserId);
            User other = iAmSender ? msg.getReceiver() : msg.getSender();

            // Compter les non lus de cet expéditeur vers moi
            long unread = messageRepository.countUnread(currentUserId);

            return ConversationSummaryResponse.builder()
                    .participantId(other.getId())
                    .participantFirstName(other.getFirstName())
                    .participantLastName(other.getLastName())
                    .participantAvatarUrl(other.getAvatarUrl())
                    .participantVerified(other.isIdentityVerified())
                    .lastMessage(msg.getContent())
                    .lastMessageAt(msg.getCreatedAt())
                    .lastMessageFromMe(iAmSender)
                    .unreadCount(unread)
                    .build();
        }).toList();
    }
*/


    // ── Liste des conversations (lecture seule) ────────────────────────────
    @Transactional(readOnly = true)
    public List<ConversationSummaryResponse> getConversations(String currentUserId) {
        return messageRepository.findLastMessagePerConversation(currentUserId).stream()
                .map(msg -> {
                    boolean iAmSender = msg.getSender().getId().equals(currentUserId);
                    User other = iAmSender ? msg.getReceiver() : msg.getSender();
                    return ConversationSummaryResponse.builder()
                            .participantId(other.getId())
                            .participantFirstName(other.getFirstName())
                            .participantLastName(other.getLastName())
                            .participantAvatarUrl(other.getAvatarUrl())
                            .participantVerified(other.isIdentityVerified())
                            .lastMessage(msg.getContent())
                            .lastMessageAt(msg.getCreatedAt())
                            .lastMessageFromMe(iAmSender)
                            .unreadCount(messageRepository.countUnread(currentUserId))
                            .build();
                }).toList();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Nombre de messages non lus (pour le badge)
    // Route : GET /messages/unread/count
    // ─────────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public long countUnread(String userId) {
        return messageRepository.countUnread(userId);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Signaler un message
    // Route : POST /messages/{id}/flag
    // ─────────────────────────────────────────────────────────────────────
    public void flagMessage(String messageId, String reason, String reporterId) {
        Message msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new InvalidOperationException("Message not found: " + messageId));

        if (msg.isDeleted())
            throw new InvalidOperationException("Cannot flag a deleted message");

        msg.flag(reason);
        messageRepository.save(msg);
        auditPort.log("MESSAGE_FLAGGED", "Message", messageId, null, reason, reporterId);
    }


    // ─────────────────────────────────────────────────────────────────────
    // Supprimer un message (soft delete, uniquement l'expéditeur)
    // Route : DELETE /messages/{id}
    // ─────────────────────────────────────────────────────────────────────

    // ── Supprimer un message (soft) ───────────────────────────────────────
    public void deleteMessage(String messageId, String requesterId) {
        Message msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new InvalidOperationException("Message introuvable: " + messageId));
        if (!msg.getSender().getId().equals(requesterId))
            throw new InvalidOperationException("Vous ne pouvez supprimer que vos propres messages");
        msg.softDelete();
        messageRepository.save(msg);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Mapper Message → MessageResponse
    // ─────────────────────────────────────────────────────────────────────
    private MessageResponse toResponse(Message m) {
        return MessageResponse.builder()
                .id(m.getId())
                .senderId(m.getSender().getId())
                .receiverId(m.getReceiver().getId())
                .content(m.getContent())
                .read(m.isRead())
                .createdAt(m.getCreatedAt()).build();
    }

    /*   public void markAsRead(String otherUserId, String currentUserId) {

           messageRepository.markConversationAsRead(otherUserId, currentUserId);
       }
   */
    // ── Marquer un message spécifique comme lu ────────────────────────────
    // Route : PATCH /messages/read/{messageId}   ← nom sans "/"
    @Transactional
    public void markAsRead(String messageId, String userId) {
        messageRepository.findById(messageId).ifPresent(msg -> {
            if (msg.getReceiver().getId().equals(userId)) {
                msg.markAsRead();
                messageRepository.save(msg);
            }
        });
    }

    // ── Marquer toute une conversation comme lue (via WebSocket) ─────────
    @Transactional
    public void markConversationAsRead(String conversationId, String userId) {
        messageRepository.markConversationAsRead(userId, conversationId);
    }

}
