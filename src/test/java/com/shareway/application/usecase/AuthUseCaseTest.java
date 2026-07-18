package com.shareway.application.usecase;

import com.shareway.application.dto.request.LoginRequest;
import com.shareway.application.dto.request.RegisterRequest;
import com.shareway.application.dto.response.AuthResponse;
import com.shareway.application.port.out.AuditPort;
import com.shareway.application.port.out.EmailPort;
import com.shareway.application.port.out.JwtPort;
import com.shareway.application.port.out.TwoFaPort;
import com.shareway.domain.exception.AccountBlockedException;
import com.shareway.domain.exception.NotAuthorizedException;
import com.shareway.domain.exception.ResourceAlreadyExistsException;
import com.shareway.domain.exception.UserNotFoundException;
import com.shareway.domain.model.User;
import com.shareway.domain.repository.AdminRoleRepository;
import com.shareway.domain.repository.PasswordResetTokenRepository;
import com.shareway.domain.repository.UserRepository;
import com.shareway.domain.service.UserDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthUseCaseTest {

    @Mock private UserRepository userRepository;
    @Mock private AdminRoleRepository adminRoleRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private UserDomainService userDomainService;
    @Mock private JwtPort jwtPort;
    @Mock private EmailPort emailPort;
    @Mock private AuditPort auditPort;
    @Mock private TwoFaPort twoFaPort;
    @Mock private UserMapper userMapper;
    @Mock private ReferralUseCase referralUseCase;

    @Captor private ArgumentCaptor<User> userCaptor;

    private AuthUseCase authUseCase;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(4);
        authUseCase = new AuthUseCase(
                userRepository, adminRoleRepository, passwordResetTokenRepository,
                userDomainService,
                passwordEncoder, jwtPort, emailPort, auditPort, twoFaPort,
                userMapper, referralUseCase
        );
    }

    @Test
    void register_shouldCreateUserAndReturnToken() {
        RegisterRequest req = new RegisterRequest();
        req.setFirstName("John");
        req.setLastName("Doe");
        req.setEmail("john@example.com");
        req.setPassword("password123");
        req.setRole("DRIVER");
        req.setPreferredLang("fr");

        when(userDomainService.generateEmailVerificationToken()).thenReturn("verify-token-123");
        when(jwtPort.generateToken(any(), any(), any(), any()))
                .thenReturn("token");
        when(jwtPort.generateRefreshToken(any()))
                .thenReturn("refresh");
        when(userMapper.toResponse(any()))
                .thenReturn(null);

        doNothing().when(emailPort).sendVerificationEmail(any(), any(), any());
        doNothing().when(auditPort).log(any(), any(), any(), any(), any(), any());

        AuthResponse response = authUseCase.register(req);

        assertNotNull(response);
        assertEquals("token", response.getToken());
        assertEquals("refresh", response.getRefreshToken());
        verify(userRepository).save(any(User.class));
        verify(emailPort).sendVerificationEmail(anyString(), anyString(), anyString());
        verify(auditPort).log(eq("USER_REGISTERED"), any(), any(), any(), any(), any());
    }

    @Test
    void register_shouldThrowWhenEmailExists() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("existing@example.com");

        doThrow(new ResourceAlreadyExistsException("Email already registered"))
                .when(userDomainService).validateRegistration(req.getEmail());

        assertThrows(ResourceAlreadyExistsException.class, () -> authUseCase.register(req));
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_withValidCredentials_shouldReturnToken() {
        User user = User.builder()
                .id("user-1")
                .email("john@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(User.UserRole.DRIVER)
                .emailVerified(true)
                .adminApproved(true)
                .build();

        when(userRepository.findByEmailAndDeletedAtIsNull("john@example.com"))
                .thenReturn(Optional.of(user));
        when(jwtPort.generateToken(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("token");
        when(jwtPort.generateRefreshToken(anyString()))
                .thenReturn("refresh");
        when(userMapper.toResponse(any()))
                .thenReturn(null);

        LoginRequest req = new LoginRequest();
        req.setEmail("john@example.com");
        req.setPassword("password123");

        AuthResponse response = authUseCase.login(req, "127.0.0.1", "test-agent");

        assertNotNull(response);
        assertEquals("token", response.getToken());
        verify(userRepository).updateLastLogin(user.getId());
        verify(auditPort).logLogin(eq(user.getId()), eq("john@example.com"), anyString(), anyString(), eq(true), isNull());
    }

    @Test
    void login_withInvalidEmail_shouldThrow() {
        when(userRepository.findByEmailAndDeletedAtIsNull("unknown@example.com"))
                .thenReturn(Optional.empty());

        LoginRequest req = new LoginRequest();
        req.setEmail("unknown@example.com");
        req.setPassword("password123");

        assertThrows(UserNotFoundException.class,
                () -> authUseCase.login(req, "127.0.0.1", "test-agent"));
    }

    @Test
    void login_withWrongPassword_shouldThrow() {
        User user = User.builder()
                .id("user-1")
                .email("john@example.com")
                .passwordHash(passwordEncoder.encode("correct-password"))
                .build();

        when(userRepository.findByEmailAndDeletedAtIsNull("john@example.com"))
                .thenReturn(Optional.of(user));

        LoginRequest req = new LoginRequest();
        req.setEmail("john@example.com");
        req.setPassword("wrong-password");

        assertThrows(NotAuthorizedException.class,
                () -> authUseCase.login(req, "127.0.0.1", "test-agent"));
    }

    @Test
    void login_withBlockedAccount_shouldThrow() {
        User user = User.builder()
                .id("user-1")
                .email("john@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .blocked(true)
                .blockReason("Spam reported")
                .build();

        when(userRepository.findByEmailAndDeletedAtIsNull("john@example.com"))
                .thenReturn(Optional.of(user));

        LoginRequest req = new LoginRequest();
        req.setEmail("john@example.com");
        req.setPassword("password123");

        assertThrows(AccountBlockedException.class,
                () -> authUseCase.login(req, "127.0.0.1", "test-agent"));
    }

    @Test
    void login_withValid2FA_shouldSucceed() {
        User user = User.builder()
                .id("user-1")
                .email("john@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .twoFaEnabled(true)
                .twoFaSecret("secret-key")
                .emailVerified(true)
                .adminApproved(true)
                .build();

        when(userRepository.findByEmailAndDeletedAtIsNull("john@example.com"))
                .thenReturn(Optional.of(user));
        when(twoFaPort.verify("secret-key", "123456")).thenReturn(true);
        when(jwtPort.generateToken(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("token");
        when(jwtPort.generateRefreshToken(anyString())).thenReturn("refresh");
        when(userMapper.toResponse(any())).thenReturn(null);

        LoginRequest req = new LoginRequest();
        req.setEmail("john@example.com");
        req.setPassword("password123");
        req.setTwoFaCode("123456");

        AuthResponse response = authUseCase.login(req, "127.0.0.1", "test-agent");

        assertNotNull(response);
        assertEquals("token", response.getToken());
    }

    @Test
    void login_withInvalid2FA_shouldThrow() {
        User user = User.builder()
                .id("user-1")
                .email("john@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .twoFaEnabled(true)
                .twoFaSecret("secret-key")
                .emailVerified(true)
                .adminApproved(true)
                .build();

        when(userRepository.findByEmailAndDeletedAtIsNull("john@example.com"))
                .thenReturn(Optional.of(user));
        when(twoFaPort.verify("secret-key", "000000")).thenReturn(false);

        LoginRequest req = new LoginRequest();
        req.setEmail("john@example.com");
        req.setPassword("password123");
        req.setTwoFaCode("000000");

        assertThrows(NotAuthorizedException.class,
                () -> authUseCase.login(req, "127.0.0.1", "test-agent"));
    }
}
