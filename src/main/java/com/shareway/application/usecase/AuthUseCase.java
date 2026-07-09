package com.shareway.application.usecase;

import com.shareway.application.dto.request.LoginRequest;
import com.shareway.application.dto.request.RegisterRequest;
import com.shareway.application.dto.response.AuthResponse;
import com.shareway.application.port.out.AuditPort;
import com.shareway.application.port.out.EmailPort;
import com.shareway.application.port.out.JwtPort;
import com.shareway.application.port.out.TwoFaPort;
import com.shareway.domain.exception.AccountBlockedException;
import com.shareway.domain.exception.InvalidOperationException;
import com.shareway.domain.exception.NotAuthorizedException;
import com.shareway.domain.exception.UserNotFoundException;
import com.shareway.domain.model.AdminRole;
import com.shareway.domain.model.SystemRole;
import com.shareway.domain.model.User;
import com.shareway.domain.repository.AdminRoleRepository;
import com.shareway.domain.repository.UserRepository;
import com.shareway.domain.service.UserDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthUseCase {

    private final UserRepository userRepository;
    private final AdminRoleRepository adminRoleRepository;
    private final UserDomainService userDomainService;
    private final PasswordEncoder passwordEncoder;
    private final JwtPort jwtPort;
    private final EmailPort emailPort;
    private final AuditPort auditPort;
    private final TwoFaPort twoFaPort;
    private final UserMapper userMapper;
    private final ReferralUseCase referralUseCase;

    public AuthResponse register(RegisterRequest req) {
        userDomainService.validateRegistration(req.getEmail());

        String verifyToken = userDomainService.generateEmailVerificationToken();

        User user = User.builder()
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .email(req.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .phone(req.getPhone())
                .role(User.UserRole.valueOf(req.getRole()))
                .preferredLang(req.getPreferredLang() != null
                        ? User.Language.valueOf(req.getPreferredLang()) : User.Language.fr)
                .emailVerifyToken(verifyToken)
                .emailVerifyExpiry(LocalDateTime.now().plusHours(24))
                .build();

        userRepository.save(user);

        if (req.getReferralCode() != null && !req.getReferralCode().isBlank()) {
            referralUseCase.applyReferralCode(req.getReferralCode(), user.getId());
        }

        emailPort.sendVerificationEmail(user.getEmail(), user.getFirstName(), verifyToken);
        auditPort.log("USER_REGISTERED", "User", user.getId(), null, user.getEmail(), user.getId());
        String systemeRole = user.getSystemRole() != null ? user.getSystemRole().name() : "";

        String token = jwtPort.generateToken(user.getId(), user.getEmail(), user.getRole().name(), systemeRole);
        String refresh = jwtPort.generateRefreshToken(user.getId());

        return AuthResponse.builder()
                .token(token).refreshToken(refresh)
                .user(userMapper.toResponse(user))
                .build();
    }

    public AuthResponse login(LoginRequest req, String ip, String userAgent) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(req.getEmail().toLowerCase())
                .orElseThrow(() -> new UserNotFoundException("Invalid credentials"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash()))
            throw new NotAuthorizedException("Invalid credentials");

        if (user.isBlocked())
            throw new AccountBlockedException("Account blocked: " + user.getBlockReason());

        if (user.isTwoFaEnabled()) {
            if (req.getTwoFaCode() == null || req.getTwoFaCode().isBlank()) {
                String sessionToken = jwtPort.generateTwoFaSessionToken(user.getId());
                return AuthResponse.builder().requiresTwoFa(true).twoFaSessionToken(sessionToken).build();
            }
            if (!twoFaPort.verify(user.getTwoFaSecret(), req.getTwoFaCode()))
                throw new NotAuthorizedException("Invalid 2FA code");
        }

        // Vérifier si cet user a un rôle admin
        Optional<AdminRole> adminRole = adminRoleRepository.findByUserId(user.getId());

        String systemeRole = adminRole
                .map(ar -> ar.getRole().name())   // ADMIN, SUPER_ADMIN, MODERATOR
                .orElse("");


        userRepository.updateLastLogin(user.getId());
        auditPort.logLogin(user.getId(), user.getEmail(), ip, userAgent, true, null);

        if (!systemeRole.isEmpty()) {
            user.setSystemRole(SystemRole.fromString(systemeRole));
        }

        String token = jwtPort.generateToken(user.getId(), user.getEmail(), user.getRole().name(), systemeRole);
        String refresh = jwtPort.generateRefreshToken(user.getId());

        return AuthResponse.builder()
                .token(token).refreshToken(refresh)
                .user(userMapper.toResponse(user))
                .build();
    }

    public void verifyEmail(String token) {
        User user = userRepository.findByEmailVerifyToken(token)
                .orElseThrow(() -> new InvalidOperationException("Invalid or expired token"));
        userDomainService.validateEmailVerification(user, token);
        user.verifyEmail();
        userRepository.save(user);
        auditPort.log("EMAIL_VERIFIED", "User", user.getId(), null, null, user.getId());
    }

    public void forgotPassword(String email) {
        userRepository.findByEmailAndDeletedAtIsNull(email.toLowerCase())
                .ifPresent(user -> {
                    String token = userDomainService.generateEmailVerificationToken();
                    emailPort.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), token);
                    auditPort.log("PASSWORD_RESET_REQUESTED", "User", user.getId(), null, null, user.getId());
                });
    }
}
