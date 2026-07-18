package com.shareway.application.usecase;

import com.shareway.application.dto.request.SendMessageRequest;
import com.shareway.application.dto.response.PageResponse;
import com.shareway.application.port.out.AuditPort;
import com.shareway.application.port.out.NotificationPort;
import com.shareway.domain.exception.InvalidOperationException;
import com.shareway.domain.exception.UserNotFoundException;
import com.shareway.domain.model.Message;
import com.shareway.domain.model.User;
import com.shareway.domain.repository.MessageRepository;
import com.shareway.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageUseCaseTest {

    @Mock private MessageRepository messageRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationPort notificationPort;
    @Mock private AuditPort auditPort;

    @InjectMocks
    private MessageUseCase messageUseCase;

    private User sender;
    private User receiver;
    private Message message;

    @BeforeEach
    void setUp() {
        sender = User.builder()
                .id("user-1").firstName("Jean").lastName("Dupont")
                .email("jean@test.com").role(User.UserRole.DRIVER).build();

        receiver = User.builder()
                .id("user-2").firstName("Marie").lastName("Niyonzima")
                .email("marie@test.com").role(User.UserRole.PASSENGER).build();

        message = Message.builder()
                .id("msg-1").sender(sender).receiver(receiver)
                .content("Bonjour !").build();
    }

    // ── SEND ───────────────────────────────────────────────────────────

    @Test
    void send_message_shouldSucceed() {
        SendMessageRequest req = new SendMessageRequest();
        req.setReceiverId("user-2");
        req.setContent("Bonjour !");

        when(userRepository.findByIdAndDeletedAtIsNull("user-1")).thenReturn(Optional.of(sender));
        when(userRepository.findByIdAndDeletedAtIsNull("user-2")).thenReturn(Optional.of(receiver));
        when(messageRepository.save(any(Message.class))).thenAnswer(i -> i.getArgument(0));

        var response = messageUseCase.send(req, "user-1");

        assertNotNull(response);
        assertEquals("Bonjour !", response.getContent());
        assertEquals("user-1", response.getSenderId());
        assertEquals("user-2", response.getReceiverId());
        verify(notificationPort).notifyWithLink(eq("user-2"), eq("MESSAGE"), anyString(), eq("Bonjour !"), anyString());
    }

    @Test
    void send_message_to_self_shouldThrow() {
        SendMessageRequest req = new SendMessageRequest();
        req.setReceiverId("user-1");
        req.setContent("Test");

        assertThrows(InvalidOperationException.class,
                () -> messageUseCase.send(req, "user-1"));
    }

    @Test
    void send_message_to_unknown_user_shouldThrow() {
        SendMessageRequest req = new SendMessageRequest();
        req.setReceiverId("unknown");
        req.setContent("Test");

        when(userRepository.findByIdAndDeletedAtIsNull("user-1")).thenReturn(Optional.of(sender));
        when(userRepository.findByIdAndDeletedAtIsNull("unknown")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> messageUseCase.send(req, "user-1"));
    }

    @Test
    void send_message_to_blocked_user_shouldThrow() {
        receiver.setBlocked(true);

        SendMessageRequest req = new SendMessageRequest();
        req.setReceiverId("user-2");
        req.setContent("Test");

        when(userRepository.findByIdAndDeletedAtIsNull("user-1")).thenReturn(Optional.of(sender));
        when(userRepository.findByIdAndDeletedAtIsNull("user-2")).thenReturn(Optional.of(receiver));

        assertThrows(InvalidOperationException.class,
                () -> messageUseCase.send(req, "user-1"));
    }

    // ── DELETE ─────────────────────────────────────────────────────────

    @Test
    void delete_own_message_shouldSucceed() {
        when(messageRepository.findById("msg-1")).thenReturn(Optional.of(message));

        messageUseCase.deleteMessage("msg-1", "user-1");

        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void delete_other_message_shouldThrow() {
        when(messageRepository.findById("msg-1")).thenReturn(Optional.of(message));

        assertThrows(InvalidOperationException.class,
                () -> messageUseCase.deleteMessage("msg-1", "user-2"));
    }

    // ── FLAG ───────────────────────────────────────────────────────────

    @Test
    void flag_message_shouldSucceed() {
        when(messageRepository.findById("msg-1")).thenReturn(Optional.of(message));

        messageUseCase.flagMessage("msg-1", "Inapproprié", "user-2");

        verify(messageRepository).save(any(Message.class));
        verify(auditPort).log(eq("MESSAGE_FLAGGED"), eq("Message"), eq("msg-1"), isNull(), eq("Inapproprié"), eq("user-2"));
    }

    // ── COUNT UNREAD ───────────────────────────────────────────────────

    @Test
    void countUnread_shouldReturnCount() {
        when(messageRepository.countUnread("user-1")).thenReturn(5L);

        long count = messageUseCase.countUnread("user-1");

        assertEquals(5L, count);
    }

    // ── GET CONVERSATION ───────────────────────────────────────────────

    @Test
    void getConversation_shouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 50);
        var page = new PageImpl<>(List.of(message), pageable, 1);

        when(userRepository.findByIdAndDeletedAtIsNull("user-2")).thenReturn(Optional.of(receiver));
        when(messageRepository.findConversation("user-1", "user-2", pageable)).thenReturn(page);

        PageResponse<?> response = messageUseCase.getConversation("user-1", "user-2", 0, 50);

        assertNotNull(response);
        verify(messageRepository).markConversationAsRead("user-1", "user-2");
    }
}
