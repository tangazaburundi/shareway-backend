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
import com.shareway.domain.model.PasswordResetToken;
import com.shareway.domain.model.SystemRole;
import com.shareway.domain.model.User;
import com.shareway.domain.repository.AdminRoleRepository;
import com.shareway.domain.repository.PasswordResetTokenRepository;
import com.shareway.domain.repository.UserRepository;
import com.shareway.domain.service.UserDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.PlatformTransactionManager;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthUseCase {

    private final UserRepository userRepository;
    private final AdminRoleRepository adminRoleRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserDomainService userDomainService;
    private final PasswordEncoder passwordEncoder;
    private final JwtPort jwtPort;
    private final EmailPort emailPort;
    private final AuditPort auditPort;
    private final TwoFaPort twoFaPort;
    private final UserMapper userMapper;
    private final ReferralUseCase referralUseCase;
    private final PlatformTransactionManager transactionManager;

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

        if (!user.isEmailVerified()) {
            var tt = new TransactionTemplate(transactionManager);
            tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            tt.executeWithoutResult(status -> {
                String verifyToken = userDomainService.generateEmailVerificationToken();
                user.setEmailVerifyToken(verifyToken);
                user.setEmailVerifyExpiry(LocalDateTime.now().plusHours(24));
                userRepository.save(user);
                emailPort.sendVerificationEmail(user.getEmail(), user.getFirstName(), verifyToken);
            });
            throw new NotAuthorizedException("Vérifiez votre email d'abord. Un nouveau lien de validation vous a été envoyé.");
        }

        if (!user.isAdminApproved())
            throw new NotAuthorizedException("Votre compte est en attente de validation par un administrateur.");

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

    public void resendVerification(String email) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(email.toLowerCase())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.isEmailVerified()) {
            throw new InvalidOperationException("Email already verified");
        }

        var tt = new TransactionTemplate(transactionManager);
        tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tt.executeWithoutResult(status -> {
            String verifyToken = userDomainService.generateEmailVerificationToken();
            user.setEmailVerifyToken(verifyToken);
            user.setEmailVerifyExpiry(LocalDateTime.now().plusHours(24));
            userRepository.save(user);
            emailPort.sendVerificationEmail(user.getEmail(), user.getFirstName(), verifyToken);
        });
        auditPort.log("EMAIL_VERIFICATION_RESENT", "User", user.getId(), null, null, user.getId());
    }

    public void forgotPassword(String email) {
        userRepository.findByEmailAndDeletedAtIsNull(email.toLowerCase())
                .ifPresent(user -> {
                    String token = userDomainService.generateEmailVerificationToken();
                    String tokenHash = hashToken(token);

                    PasswordResetToken resetToken = PasswordResetToken.builder()
                            .user(user)
                            .tokenHash(tokenHash)
                            .expiresAt(LocalDateTime.now().plusHours(1))
                            .build();
                    passwordResetTokenRepository.save(resetToken);

                    emailPort.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), token);
                    auditPort.log("PASSWORD_RESET_REQUESTED", "User", user.getId(), null, null, user.getId());
                });
    }

    public void resetPassword(String token, String newPassword) {
        String tokenHash = hashToken(token);
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHashAndUsedFalse(tokenHash)
                .orElseThrow(() -> new InvalidOperationException("Invalid or expired reset token"));

        if (resetToken.isExpired()) {
            throw new InvalidOperationException("Reset token has expired");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.markUsed();
        passwordResetTokenRepository.save(resetToken);

        auditPort.log("PASSWORD_RESET_COMPLETED", "User", user.getId(), null, null, user.getId());
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtPort.isValid(refreshToken)) {
            throw new NotAuthorizedException("Invalid refresh token");
        }

        String userId = jwtPort.extractUserId(refreshToken);
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.isBlocked()) {
            throw new AccountBlockedException("Account blocked");
        }

        Optional<AdminRole> adminRole = adminRoleRepository.findByUserId(user.getId());
        String systemeRole = adminRole.map(ar -> ar.getRole().name()).orElse("");

        String newToken = jwtPort.generateToken(user.getId(), user.getEmail(), user.getRole().name(), systemeRole);
        String newRefresh = jwtPort.generateRefreshToken(user.getId());

        return AuthResponse.builder()
                .token(newToken).refreshToken(newRefresh)
                .user(userMapper.toResponse(user))
                .build();
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
