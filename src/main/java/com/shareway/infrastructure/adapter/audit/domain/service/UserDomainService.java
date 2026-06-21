package com.shareway.infrastructure.adapter.audit.domain.service;

import com.shareway.infrastructure.adapter.audit.domain.exception.InvalidOperationException;
import com.shareway.infrastructure.adapter.audit.domain.exception.ResourceAlreadyExistsException;
import com.shareway.infrastructure.adapter.audit.domain.exception.UserNotFoundException;
import com.shareway.infrastructure.adapter.audit.domain.model.User;
import com.shareway.infrastructure.adapter.audit.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service domaine pour la logique métier utilisateur.
 */
@Service
@RequiredArgsConstructor
public class UserDomainService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final UserRepository userRepository;

    public void validateRegistration(String email) {
        if (userRepository.existsByEmail(email))
            throw new ResourceAlreadyExistsException("Email already registered: " + email);
    }

    public String generateEmailVerificationToken() {
        return UUID.randomUUID().toString();
    }

    public String generatePhoneOtp() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    public void validateEmailVerification(User user, String token) {
        if (user.getEmailVerifyToken() == null || !user.getEmailVerifyToken().equals(token))
            throw new InvalidOperationException("Invalid verification token");
        if (user.getEmailVerifyExpiry() != null && user.getEmailVerifyExpiry().isBefore(LocalDateTime.now()))
            throw new InvalidOperationException("Verification token has expired");
    }

    public void validatePhoneOtp(User user, String otp) {
        if (user.getPhoneOtp() == null || !user.getPhoneOtp().equals(otp))
            throw new InvalidOperationException("Invalid OTP code");
        if (user.getPhoneOtpExpiry() != null && user.getPhoneOtpExpiry().isBefore(LocalDateTime.now()))
            throw new InvalidOperationException("OTP has expired");
    }

    public User findActiveUser(String userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
    }
}
