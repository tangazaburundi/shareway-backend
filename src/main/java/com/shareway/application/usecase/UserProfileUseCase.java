package com.shareway.application.usecase;

import com.shareway.application.dto.request.ChangePasswordRequest;
import com.shareway.application.dto.request.TwoFaVerifyRequest;
import com.shareway.application.dto.request.UpdateProfileRequest;
import com.shareway.application.dto.response.TravelPreferencesResponse;
import com.shareway.application.dto.response.UserResponse;
import com.shareway.application.dto.response.VehicleResponse;
import com.shareway.application.port.out.AuditPort;
import com.shareway.application.port.out.EmailPort;
import com.shareway.application.port.out.StoragePort;
import com.shareway.application.port.out.TwoFaPort;
import com.shareway.infrastructure.adapter.audit.domain.exception.InvalidOperationException;
import com.shareway.infrastructure.adapter.audit.domain.exception.NotAuthorizedException;
import com.shareway.infrastructure.adapter.audit.domain.exception.UserNotFoundException;
import com.shareway.infrastructure.adapter.audit.domain.model.User;
import com.shareway.infrastructure.adapter.audit.domain.model.UserTravelPreferences;
import com.shareway.infrastructure.adapter.audit.domain.model.Vehicle;
import com.shareway.infrastructure.adapter.audit.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserProfileUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StoragePort storagePort;
    private final EmailPort emailPort;
    private final AuditPort auditPort;
    private final TwoFaPort twoFaPort;

    @Transactional(readOnly = true)
    public UserResponse getProfile(String userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return toResponse(user);
    }

    public UserResponse updateProfile(UpdateProfileRequest req, String userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (req.getFirstName() != null) user.setFirstName(req.getFirstName());
        if (req.getLastName() != null) user.setLastName(req.getLastName());
        if (req.getPhone() != null) user.setPhone(req.getPhone());
        user.setPhoneVisible(req.isPhoneVisible());
        if (req.getBio() != null) user.setBio(req.getBio());
        if (req.getPreferredLang() != null)
            user.setPreferredLang(User.Language.valueOf(req.getPreferredLang()));
        if (req.getRole() != null)
            user.setRole(User.UserRole.valueOf(req.getRole()));

        if (req.getPreferences() != null && user.getTravelPreferences() != null) {
            UserTravelPreferences prefs = user.getTravelPreferences();
            prefs.setMusic(req.getPreferences().isMusic());
            prefs.setSmoking(req.getPreferences().isSmoking());
            prefs.setPets(req.getPreferences().isPets());
            prefs.setTalking(req.getPreferences().isTalking());
        } else if (req.getPreferences() != null) {
            UserTravelPreferences prefs = UserTravelPreferences.builder()
                    .user(user)
                    .music(req.getPreferences().isMusic())
                    .smoking(req.getPreferences().isSmoking())
                    .pets(req.getPreferences().isPets())
                    .talking(req.getPreferences().isTalking())
                    .build();
            user.setTravelPreferences(prefs);
        }

        userRepository.save(user);
        auditPort.log("PROFILE_UPDATED", "User", userId, null, null, userId);
        return toResponse(user);
    }

    public String uploadAvatar(MultipartFile file, String userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (file.isEmpty()) throw new InvalidOperationException("File is empty");
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/"))
            throw new InvalidOperationException("Only image files are allowed");
        if (file.getSize() > 5 * 1024 * 1024)
            throw new InvalidOperationException("File size must be under 5MB");

        if (user.getAvatarUrl() != null) {
            try {
                storagePort.delete(user.getAvatarUrl());
            } catch (Exception ignored) {
            }
        }

        String url = storagePort.upload(file, "avatars/" + userId);
        user.setAvatarUrl(url);
        userRepository.save(user);
        return url;
    }

    public void changePassword(ChangePasswordRequest req, String userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPasswordHash()))
            throw new NotAuthorizedException("Current password is incorrect");

        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
        emailPort.sendGeneral(user.getEmail(), "Password changed",
                "Your password was changed successfully. If this wasn't you, contact support immediately.");
        auditPort.log("PASSWORD_CHANGED", "User", userId, null, null, userId);
    }

    // ===== 2FA =====
    public java.util.Map<String, String> setup2Fa(String userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        String secret = twoFaPort.generateSecret();
        String uri = twoFaPort.generateQrCodeUri(secret, user.getEmail(), "Shareway");
        String qrCode = twoFaPort.generateQrCodeBase64(uri);

        // Store secret temporarily (not enabled yet until verified)
        user.setTwoFaSecret(secret);
        userRepository.save(user);

        return java.util.Map.of("secret", secret, "qrCode", qrCode);
    }

    public void enable2Fa(TwoFaVerifyRequest req, String userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.getTwoFaSecret() == null)
            throw new InvalidOperationException("2FA setup not started. Call /2fa/setup first.");

        if (!twoFaPort.verify(user.getTwoFaSecret(), req.getCode()))
            throw new NotAuthorizedException("Invalid 2FA code");

        user.setTwoFaEnabled(true);
        userRepository.save(user);
        auditPort.log("2FA_ENABLED", "User", userId, null, null, userId);
        emailPort.sendGeneral(user.getEmail(), "2FA Enabled",
                "Two-factor authentication has been enabled on your account.");
    }

    public void disable2Fa(TwoFaVerifyRequest req, String userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!user.isTwoFaEnabled())
            throw new InvalidOperationException("2FA is not enabled");

        if (!twoFaPort.verify(user.getTwoFaSecret(), req.getCode()))
            throw new NotAuthorizedException("Invalid 2FA code");

        user.setTwoFaEnabled(false);
        user.setTwoFaSecret(null);
        userRepository.save(user);
        auditPort.log("2FA_DISABLED", "User", userId, null, null, userId);
    }

    private UserResponse toResponse(User u) {
        VehicleResponse vehicle = u.getVehicles().stream()
                .filter(Vehicle::isActive).findFirst()
                .map(v -> VehicleResponse.builder()
                        .id(v.getId()).brand(v.getBrand()).model(v.getModel())
                        .color(v.getColor()).licensePlate(v.getLicensePlate())
                        .year(v.getYear()).photoUrl(v.getPhotoUrl()).build())
                .orElse(null);

        TravelPreferencesResponse prefs = u.getTravelPreferences() != null
                ? TravelPreferencesResponse.builder()
                .music(u.getTravelPreferences().isMusic())
                .smoking(u.getTravelPreferences().isSmoking())
                .pets(u.getTravelPreferences().isPets())
                .talking(u.getTravelPreferences().isTalking()).build()
                : null;

        return UserResponse.builder()
                .id(u.getId()).firstName(u.getFirstName()).lastName(u.getLastName())
                .email(u.getEmail()).phone(u.getPhone()).phoneVisible(u.isPhoneVisible())
                .avatarUrl(u.getAvatarUrl()).bio(u.getBio()).role(u.getRole().name())
                .preferredLang(u.getPreferredLang().name())
                .emailVerified(u.isEmailVerified()).phoneVerified(u.isPhoneVerified())
                .identityVerified(u.isIdentityVerified()).twoFaEnabled(u.isTwoFaEnabled())
                .active(u.isActive()).blocked(u.isBlocked()).blockReason(u.getBlockReason())
                .rating(u.getRating()).reviewCount(u.getReviewCount())
                .vehicle(vehicle).preferences(prefs)
                .createdAt(u.getCreatedAt()).lastLoginAt(u.getLastLoginAt())
                .build();
    }
}
