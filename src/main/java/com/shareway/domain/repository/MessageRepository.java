package com.shareway.domain.repository;

import com.shareway.domain.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, String> {

    // ── Conversation entre deux utilisateurs (bidirectionnelle) ──────────
    @Query("""
            SELECT m FROM Message m
            WHERE ((m.sender.id = :a AND m.receiver.id = :b)
                OR (m.sender.id = :b AND m.receiver.id = :a))
              AND m.deletedAt IS NULL
            ORDER BY m.createdAt ASC
            """)
    Page<Message> findConversation(@Param("a") String userId1,
                                   @Param("b") String userId2,
                                   Pageable pageable);

    // ── Liste des conversations distinctes d'un utilisateur ─────────────
    // Retourne le dernier message de chaque conversation
    @Query(value = """
            SELECT m.* FROM messages m
            INNER JOIN (
                SELECT
                    LEAST(sender_id, receiver_id)   AS user_a,
                    GREATEST(sender_id, receiver_id) AS user_b,
                    MAX(created_at)                  AS last_msg
                FROM messages
                WHERE (sender_id = :userId OR receiver_id = :userId)
                  AND deleted_at IS NULL
                GROUP BY LEAST(sender_id, receiver_id), GREATEST(sender_id, receiver_id)
            ) latest
              ON LEAST(m.sender_id, m.receiver_id)    = latest.user_a
             AND GREATEST(m.sender_id, m.receiver_id) = latest.user_b
             AND m.created_at = latest.last_msg
             AND m.deleted_at IS NULL
            ORDER BY m.created_at DESC
            """, nativeQuery = true)
    List<Message> findLastMessagePerConversation(@Param("userId") String userId);

    // ── Messages non lus ─────────────────────────────────────────────────
    @Query("SELECT COUNT(m) FROM Message m WHERE m.receiver.id = :userId AND m.read = false AND m.deletedAt IS NULL")
    long countUnread(@Param("userId") String userId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.receiver.id = :userId AND m.sender.id = :otherUserId AND m.readAt IS NULL AND m.deletedAt IS NULL")
    long countUnreadInConversation(@Param("userId") String userId, @Param("otherUserId") String otherUserId);

    // ── Marquer une conversation entière comme lue ───────────────────────
    @Modifying
    @Query("""
            UPDATE Message m SET m.read = true, m.readAt = CURRENT_TIMESTAMP
            WHERE m.receiver.id = :receiverId
              AND m.sender.id   = :senderId
              AND m.read = false
              AND m.deletedAt IS NULL
            """)
    void markConversationAsRead(@Param("receiverId") String receiverId,
                                @Param("senderId") String senderId);

    // ── Modération admin ─────────────────────────────────────────────────
    @Query("SELECT m FROM Message m WHERE m.flagged = true AND m.deletedAt IS NULL ORDER BY m.createdAt DESC")
    Page<Message> findFlagged(Pageable pageable);
}
