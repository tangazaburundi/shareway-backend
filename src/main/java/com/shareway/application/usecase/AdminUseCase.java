package com.shareway.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shareway.application.dto.request.AdminBlockUserRequest;
import com.shareway.application.dto.request.AdminLoginRequest;
import com.shareway.application.dto.request.AdminReviewReportRequest;
import com.shareway.application.dto.response.AdminAuthResponse;
import com.shareway.application.dto.response.AuditLogResponse;
import com.shareway.application.dto.response.MessageResponse;
import com.shareway.application.dto.response.PageResponse;
import com.shareway.application.dto.response.ReportResponse;
import com.shareway.application.dto.response.ReviewAuthorResponse;
import com.shareway.application.dto.response.ReviewResponse;
import com.shareway.application.dto.response.UserResponse;
import com.shareway.application.dto.response.TripResponse;
import com.shareway.application.dto.response.TripUserResponse;
import com.shareway.application.port.out.AuditPort;
import com.shareway.application.port.out.EmailPort;
import com.shareway.application.port.out.ExportPort;
import com.shareway.application.port.out.NotificationPort;
import com.shareway.application.port.out.TwoFaPort;
import com.shareway.domain.exception.AccountBlockedException;
import com.shareway.domain.exception.InvalidOperationException;
import com.shareway.domain.exception.NotAuthorizedException;
import com.shareway.domain.exception.ReviewNotFoundException;
import com.shareway.domain.exception.TripNotFoundException;
import com.shareway.domain.exception.UserNotFoundException;
import com.shareway.domain.model.AdminRole;
import com.shareway.domain.model.Message;
import com.shareway.domain.model.Report;
import com.shareway.domain.model.Review;
import com.shareway.domain.model.RoleRequest;
import com.shareway.domain.model.Trip;
import com.shareway.domain.model.User;
import com.shareway.domain.repository.AdminRoleRepository;
import com.shareway.domain.repository.AuditLogRepository;
import com.shareway.domain.repository.BookingRepository;
import com.shareway.domain.repository.MessageRepository;
import com.shareway.domain.repository.ReportRepository;
import com.shareway.domain.repository.ReviewRepository;
import com.shareway.domain.repository.RoleRequestRepository;
import com.shareway.domain.repository.SystemSettingRepository;
import com.shareway.domain.repository.TripRepository;
import com.shareway.domain.repository.UserRepository;
import com.shareway.infrastructure.adapter.specification.UserSpecifications;
import com.shareway.infrastructure.security.AdminJwtService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Authentification administrateur — sécurité maximale.
 * <p>
 * Mesures appliquées :
 * 1. Vérification du rôle admin AVANT toute vérification de mot de passe
 * (évite de révéler si un compte "normal" existe via l'endpoint admin —
 * réponse générique "Invalid credentials" dans tous les cas).
 * 2. Verrouillage de compte après N tentatives échouées (lockout temporaire),
 * basé sur les colonnes failed_login_attempts / locked_until de `users`.
 * 3. 2FA TOTP obligatoire si activée (two_fa_enabled / two_fa_secret).
 * 4. Audit complet de CHAQUE tentative (succès/échec) avec IP + user-agent
 * dans admin_login_audit, y compris la raison de l'échec (sans exposer
 * d'info sensible au client).
 * 5. JWT admin signé avec une clé DÉDIÉE, courte durée de vie (30 min),
 * + refresh token séparé stocké hashé (SHA-256) en base, révocable.
 * 6. Timing-safe : bcrypt est exécuté même si l'utilisateur n'existe pas
 * (compare contre un hash factice) pour éviter le timing attack
 * permettant de deviner si un email existe.
 * 7. Message d'erreur strictement générique côté client.
 */

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class AdminUseCase {

    /**
     * Hash bcrypt factice — utilisé pour égaliser le temps de réponse
     */
    private static final String DUMMY_HASH =
            "$2a$12$C6UzMDM.H6dfI/f/IKcEeuYVQ.9N1zljfMOEAm3WJsX8m1tNNB0Hu";
    private static final String GENERIC_ERROR = "Identifiants invalides";
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final MessageRepository messageRepository;
    private final ReportRepository reportRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationPort notificationPort;
    private final AuditPort auditPort;
    private final ExportPort exportPort;
    private final EmailPort emailPort;
    private final AdminRoleRepository adminRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminJwtService adminJwtService;
    private final TwoFaPort twoFaPort;
    private final RoleRequestRepository roleRequestRepository;
    private final EntityManager em;
    private final ObjectMapper objectMapper;
    private final SystemSettingRepository systemSettingRepository;
    @Value("${security.admin-auth.max-attempts:5}")
    private int maxAttempts;
    @Value("${security.admin-auth.lock-minutes:15}")
    private int lockMinutes;

    public AdminAuthResponse login(AdminLoginRequest req, String ip, String userAgent) {
        String email = req.getEmail().trim().toLowerCase();

        var userOpt = userRepository.findByEmailAndDeletedAtIsNull(email);

        // ── 1. Utilisateur introuvable : exécuter bcrypt quand même (timing) ─
        if (userOpt.isEmpty()) {
            passwordEncoder.matches(req.getPassword(), DUMMY_HASH);
            audit(null, email, false, "USER_NOT_FOUND", ip, userAgent);
            throw new NotAuthorizedException(GENERIC_ERROR);
        }

        User user = userOpt.get();

        // ── 2. Doit avoir un rôle admin ──────────────────────────────────────
        AdminRole adminRole = adminRoleRepository.findByUserId(user.getId()).orElse(null);
        if (adminRole == null) {
            passwordEncoder.matches(req.getPassword(), DUMMY_HASH);
            audit(user.getId(), email, false, "NOT_ADMIN", ip, userAgent);
            throw new NotAuthorizedException(GENERIC_ERROR);
        }

        // ── 3. Compte verrouillé (anti brute-force) ──────────────────────────
        if (user.isLocked()) {
            audit(user.getId(), email, false, "ACCOUNT_LOCKED", ip, userAgent);
            throw new AccountBlockedException(
                    "Compte temporairement verrouillé suite à plusieurs tentatives échouées. " +
                            "Réessayez après " + user.getLockedUntil());
        }

        // ── 4. Compte bloqué par un autre admin / désactivé ──────────────────
        if (user.isBlocked() || !user.isActive()) {
            audit(user.getId(), email, false, "ACCOUNT_DISABLED", ip, userAgent);
            throw new AccountBlockedException("Compte désactivé. Contactez un super-administrateur.");
        }

        // ── 5. Vérification mot de passe ─────────────────────────────────────
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            user.registerFailedLogin(maxAttempts, lockMinutes);
            userRepository.save(user);
            audit(user.getId(), email, false, "BAD_PASSWORD", ip, userAgent);

            if (user.isLocked()) {
                throw new AccountBlockedException(
                        "Compte verrouillé pour " + lockMinutes + " minutes suite à trop de tentatives échouées.");
            }
            throw new NotAuthorizedException(GENERIC_ERROR);
        }

        // ── 6. 2FA obligatoire si activée ─────────────────────────────────────
        if (user.isTwoFaEnabled()) {
            if (req.getTwoFaCode() == null || req.getTwoFaCode().isBlank()) {
                String sessionToken = adminJwtService.generateTwoFaSessionToken(user.getId());
                audit(user.getId(), email, true, "PASSWORD_OK_2FA_REQUIRED", ip, userAgent);
                return AdminAuthResponse.builder()
                        .requiresTwoFa(true)
                        .twoFaSessionToken(sessionToken)
                        .build();
            }
            if (!twoFaPort.verify(user.getTwoFaSecret(), req.getTwoFaCode())) {
                user.registerFailedLogin(maxAttempts, lockMinutes);
                userRepository.save(user);
                audit(user.getId(), email, false, "BAD_2FA_CODE", ip, userAgent);
                throw new NotAuthorizedException("Code de vérification invalide");
            }
        }

        // ── 7. Succès : reset compteur, login timestamp, audit ───────────────
        user.resetFailedLogins();
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        audit(user.getId(), email, true, "LOGIN_SUCCESS", ip, userAgent);

        return buildAuthResponse(user, adminRole, ip, userAgent);
    }

    /**
     * Rafraîchir un access token à partir d'un refresh token valide.
     * Le refresh token est vérifié contre son hash stocké en base (révocable).
     */
    public AdminAuthResponse refresh(String refreshToken, String ip, String userAgent) {
        if (!adminJwtService.isValid(refreshToken, "ADMIN_REFRESH"))
            throw new NotAuthorizedException("Session expirée, veuillez vous reconnecter");

        String userId = adminJwtService.extractUserId(refreshToken);
        String hash = sha256(refreshToken);

        Object exists = em.createNativeQuery(
                        "SELECT id FROM admin_refresh_tokens " +
                                "WHERE user_id = ?1 AND token_hash = ?2 AND revoked = 0 AND expires_at > NOW()")
                .setParameter(1, userId)
                .setParameter(2, hash)
                .getResultList()
                .stream().findFirst().orElse(null);

        if (exists == null)
            throw new NotAuthorizedException("Session invalide ou révoquée");

        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable"));
        if (user.isBlocked() || !user.isActive())
            throw new AccountBlockedException("Compte désactivé");

        AdminRole adminRole = adminRoleRepository.findByUserId(userId)
                .orElseThrow(() -> new NotAuthorizedException("Privilèges administrateur révoqués"));

        // Révoquer l'ancien refresh token (rotation)
        em.createNativeQuery("UPDATE admin_refresh_tokens SET revoked = 1 WHERE token_hash = ?1")
                .setParameter(1, hash).executeUpdate();

        return buildAuthResponse(user, adminRole, ip, userAgent);
    }

    /**
     * Révoque tous les refresh tokens de l'utilisateur (déconnexion globale)
     */
    public void logoutAll(String userId) {
        em.createNativeQuery("UPDATE admin_refresh_tokens SET revoked = 1 WHERE user_id = ?1")
                .setParameter(1, userId).executeUpdate();
        auditPort.log("ADMIN_LOGOUT_ALL", "User", userId, null, null, userId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private AdminAuthResponse buildAuthResponse(User user, AdminRole adminRole, String ip, String userAgent) {
        Set<String> permissions = parsePermissions(adminRole.getPermissions());

        String accessToken = adminJwtService.generateAccessToken(
                user.getId(), user.getEmail(), adminRole.getRole().name(), permissions);
        String refreshToken = adminJwtService.generateRefreshToken(user.getId());

        storeRefreshToken(user.getId(), refreshToken, ip, userAgent);

        return AdminAuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(adminJwtService.accessTtlSeconds())
                .requiresTwoFa(false)
                .admin(AdminAuthResponse.AdminProfile.builder()
                        .id(user.getId())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .email(user.getEmail())
                        .role(adminRole.getRole().name())
                        .permissions(permissions)
                        .build())
                .build();
    }

    private void storeRefreshToken(String userId, String refreshToken, String ip, String userAgent) {
        String hash = sha256(refreshToken);
        em.createNativeQuery(
                        "INSERT INTO admin_refresh_tokens (id, user_id, token_hash, expires_at, ip_address, user_agent) " +
                                "VALUES (UUID(), ?1, ?2, DATE_ADD(NOW(), INTERVAL 12 HOUR), ?3, ?4)")
                .setParameter(1, userId)
                .setParameter(2, hash)
                .setParameter(3, truncate(ip, 45))
                .setParameter(4, truncate(userAgent, 500))
                .executeUpdate();
    }

    private void audit(String userId, String email, boolean success, String reason, String ip, String userAgent) {
        em.createNativeQuery(
                        "INSERT INTO admin_login_audit (id, user_id, email, success, reason, ip_address, user_agent) " +
                                "VALUES (UUID(), ?1, ?2, ?3, ?4, ?5, ?6)")
                .setParameter(1, userId)
                .setParameter(2, email)
                .setParameter(3, success)
                .setParameter(4, reason)
                .setParameter(5, truncate(ip, 45))
                .setParameter(6, truncate(userAgent, 500))
                .executeUpdate();

        if (!success) {
            log.warn("Admin login failed: email={} reason={} ip={}", email, reason, ip);
        }
    }

    @SuppressWarnings("unchecked")
    private Set<String> parsePermissions(String json) {
        if (json == null || json.isBlank()) return new HashSet<>();
        try {
            return new HashSet<>(objectMapper.readValue(json, Set.class));
        } catch (Exception e) {
            return new HashSet<>();
        }
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(digest.digest(input.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }


    ////////////////////////////


    // ===== DASHBOARD =====
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboard() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalActiveUsers", userRepository.countActiveUsers());
        stats.put("newUsersToday", userRepository.countNewUsersToday());
        stats.put("openTrips", tripRepository.countOpen());
        stats.put("completedTrips", tripRepository.findByStatus(Trip.TripStatus.COMPLETED, Pageable.unpaged()).getTotalElements());
        stats.put("bookingsToday", bookingRepository.countToday());
        stats.put("pendingReports", reportRepository.countPending());
        stats.put("flaggedReviews", reviewRepository.findFlagged(Pageable.unpaged()).getTotalElements());
        stats.put("flaggedMessages", messageRepository.findFlagged(Pageable.unpaged()).getTotalElements());
        return stats;
    }


    // ===== USER MANAGEMENT =====
  /*  @Transactional(readOnly = true)

    public PageResponse<UserResponse> getUsers(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> users = userRepository.findAllActive(pageable);
        return PageResponse.from(users.map(this::toUserResponse));
    }
*/

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getUsers(int page, int size, String search) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("createdAt").descending()
        );

        Specification<User> spec = UserSpecifications.searchActiveUsers(search);

        Page<User> users = userRepository.findAll(spec, pageable);

        return PageResponse.from(users.map(this::toUserResponse));
    }

    public UserResponse blockUser(String userId, AdminBlockUserRequest req, String adminId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        user.block(req.getReason(), adminId);
        userRepository.save(user);
        notificationPort.notify(userId, "SYSTEM", "Account suspended", "Your account has been suspended.");
        auditPort.log("USER_BLOCKED", "User", userId, null, req.getReason(), adminId);
        return toUserResponse(user);
    }

    public UserResponse unblockUser(String userId, String adminId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        user.unblock();
        userRepository.save(user);
        notificationPort.notify(userId, "SYSTEM", "Account restored", "Your account has been restored.");
        auditPort.log("USER_UNBLOCKED", "User", userId, null, null, adminId);
        return toUserResponse(user);
    }

    public UserResponse verifyIdentity(String userId, String adminId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        user.verifyIdentity(adminId);
        userRepository.save(user);
        notificationPort.notify(userId, "IDENTITY_VERIFIED", "Identity verified",
                "Your identity has been verified. You can now access all features.");
        auditPort.log("IDENTITY_VERIFIED", "User", userId, null, null, adminId);
        return toUserResponse(user);
    }

    public void deleteUser(String userId, String adminId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        user.softDelete(adminId);
        userRepository.save(user);
        auditPort.log("USER_DELETED", "User", userId, user.getEmail(), null, adminId);
    }

    // ===== MODERATION =====
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getFlaggedReviews(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return PageResponse.from(reviewRepository.findFlagged(pageable).map(this::toReviewResponse));
    }

    public ReviewResponse approveReview(String reviewId, String adminId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found"));
        review.approve(adminId);
        reviewRepository.save(review);
        auditPort.log("REVIEW_APPROVED", "Review", reviewId, null, null, adminId);
        return toReviewResponse(review);
    }

    public ReviewResponse rejectReview(String reviewId, String adminId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found"));
        review.reject(adminId);
        reviewRepository.save(review);
        auditPort.log("REVIEW_REJECTED", "Review", reviewId, null, null, adminId);
        return toReviewResponse(review);
    }

    @Transactional(readOnly = true)
    public PageResponse<MessageResponse> getFlaggedMessages(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return PageResponse.from(messageRepository.findFlagged(pageable).map(this::toMessageResponse));
    }

    // ===== REPORTS =====
    @Transactional(readOnly = true)
    public PageResponse<ReportResponse> getReports(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Report> reports = status != null
                ? reportRepository.findByStatusOrderByCreatedAtDesc(Report.ReportStatus.valueOf(status), pageable)
                : reportRepository.findByOrderByCreatedAtDesc(pageable);
        return PageResponse.from(reports.map(this::toReportResponse));
    }

    public ReportResponse reviewReport(String reportId, AdminReviewReportRequest req, String adminId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new InvalidOperationException("Report not found"));
        report.review(adminId, Report.ReportStatus.valueOf(req.getStatus()), req.getActionTaken());
        reportRepository.save(report);
        auditPort.log("REPORT_REVIEWED", "Report", reportId, null, req.getStatus(), adminId);
        return toReportResponse(report);
    }

    // ===== EXPORT =====
    @Transactional(readOnly = true)
    public byte[] exportUsersCsv() {
        List<User> users = userRepository.findAllActive(Pageable.unpaged()).getContent();
        String[] headers = {"ID", "FirstName", "LastName", "Email", "Role", "EmailVerified", "IdentityVerified", "Rating", "CreatedAt"};
        List<Map<String, Object>> data = users.stream().map(u -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("ID", u.getId());
            row.put("FirstName", u.getFirstName());
            row.put("LastName", u.getLastName());
            row.put("Email", u.getEmail());
            row.put("Role", u.getRole());
            row.put("EmailVerified", u.isEmailVerified());
            row.put("IdentityVerified", u.isIdentityVerified());
            row.put("Rating", u.getRating());
            row.put("CreatedAt", u.getCreatedAt());
            return row;
        }).collect(Collectors.toList());
        return exportPort.toCsv(data, headers);
    }

    @Transactional(readOnly = true)
    public byte[] exportUsersExcel() {
        List<User> users = userRepository.findAllActive(Pageable.unpaged()).getContent();
        String[] headers = {"ID", "FirstName", "LastName", "Email", "Role", "EmailVerified", "IdentityVerified", "Rating", "CreatedAt"};
        List<Map<String, Object>> data = users.stream().map(u -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("ID", u.getId());
            row.put("FirstName", u.getFirstName());
            row.put("LastName", u.getLastName());
            row.put("Email", u.getEmail());
            row.put("Role", u.getRole());
            row.put("EmailVerified", u.isEmailVerified());
            row.put("IdentityVerified", u.isIdentityVerified());
            row.put("Rating", u.getRating());
            row.put("CreatedAt", u.getCreatedAt());
            return row;
        }).collect(Collectors.toList());
        return exportPort.toExcel(data, "Users", headers);
    }

    // ===== AUDIT =====
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> getAuditLogs(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        var logs = userId != null
                ? auditLogRepository.findByActorIdOrderByCreatedAtDesc(userId, pageable)
                : auditLogRepository.findAll(pageable);
        return PageResponse.from(logs.map(l -> AuditLogResponse.builder()
                .id(l.getId()).action(l.getAction()).entityType(l.getEntityType())
                .entityId(l.getEntityId()).actorEmail(l.getActorEmail())
                .actorRole(l.getActorRole()).ipAddress(l.getIpAddress())
                .success(l.isSuccess()).createdAt(l.getCreatedAt()).build()));
    }

    // ===== Mappers privés =====
    private UserResponse toUserResponse(User u) {
        return UserResponse.builder()
                .id(u.getId()).firstName(u.getFirstName()).lastName(u.getLastName())
                .email(u.getEmail()).phone(u.getPhone()).role(u.getRole().name())
                .emailVerified(u.isEmailVerified()).phoneVerified(u.isPhoneVerified())
                .identityVerified(u.isIdentityVerified()).active(u.isActive())
                .blocked(u.isBlocked()).blockReason(u.getBlockReason())
                .adminApproved(u.isAdminApproved())
                .rating(u.getRating()).reviewCount(u.getReviewCount())
                .createdAt(u.getCreatedAt()).lastLoginAt(u.getLastLoginAt()).build();
    }

    private ReviewResponse toReviewResponse(Review r) {
        return ReviewResponse.builder().id(r.getId()).tripId(r.getTrip().getId())
                .author(ReviewAuthorResponse.builder().id(r.getAuthor().getId())
                        .firstName(r.getAuthor().getFirstName()).lastName(r.getAuthor().getLastName())
                        .avatarUrl(r.getAuthor().getAvatarUrl()).build())
                .targetUserId(r.getTargetUser().getId()).rating(r.getRating())
                .comment(r.getComment()).type(r.getType().name())
                .flagged(r.isFlagged()).approved(r.isApproved()).createdAt(r.getCreatedAt()).build();
    }

    private MessageResponse toMessageResponse(Message m) {
        return MessageResponse.builder().id(m.getId())
                .senderId(m.getSender().getId()).receiverId(m.getReceiver().getId())
                .content(m.getContent()).read(m.isRead()).createdAt(m.getCreatedAt()).build();
    }

    private ReportResponse toReportResponse(Report r) {
        return ReportResponse.builder().id(r.getId())
                .targetType(r.getTargetType().name()).targetId(r.getTargetId())
                .reason(r.getReason().name()).description(r.getDescription())
                .status(r.getStatus().name()).actionTaken(r.getActionTaken())
                .createdAt(r.getCreatedAt()).build();
    }

    // ═══════════════════════════════════════════════════════════
    // APPROBATION / REJET UTILISATEURS
    // ═══════════════════════════════════════════════════════════

    public UserResponse approveUser(String userId, String adminId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable: " + userId));
        user.approveByAdmin(adminId);
        userRepository.save(user);
        notificationPort.notify(userId, "SYSTEM", "Compte approuvé",
                "Votre compte a été approuvé par un administrateur. Vous pouvez maintenant vous connecter.");
        emailPort.sendGeneral(user.getEmail(), "Votre compte ShareWay a été approuvé",
                "Bonjour " + user.getFirstName() + ",\n\nVotre compte a été approuvé par un administrateur. " +
                        "Vous pouvez maintenant vous connecter et utiliser toutes les fonctionnalités de ShareWay.");
        auditPort.log("USER_APPROVED", "User", userId, null, null, adminId);
        return toUserResponse(user);
    }

    public UserResponse rejectUser(String userId, String adminId, String reason) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable: " + userId));
        user.rejectByAdmin();
        userRepository.save(user);
        notificationPort.notify(userId, "SYSTEM", "Compte rejeté",
                "Votre compte a été rejeté par un administrateur." + (reason != null ? " Raison: " + reason : ""));
        emailPort.sendGeneral(user.getEmail(), "Votre compte ShareWay n'a pas été approuvé",
                "Bonjour " + user.getFirstName() + ",\n\nVotre compte n'a pas été approuvé par un administrateur." +
                        (reason != null ? "\nRaison: " + reason : "") +
                        "\n\nSi vous pensez qu'il s'agit d'une erreur, contactez le support.");
        auditPort.log("USER_REJECTED", "User", userId, null, reason, adminId);
        return toUserResponse(user);
    }

    // ═══════════════════════════════════════════════════════════
    // CHANGEMENT DE RÔLE UTILISATEUR
    // ═══════════════════════════════════════════════════════════

    public UserResponse changeUserRole(String userId, String newRole, String adminId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable: " + userId));
        User.UserRole oldRole = user.getRole();
        user.setRole(User.UserRole.valueOf(newRole));
        userRepository.save(user);
        auditPort.log("ROLE_CHANGED", "User", userId, oldRole.name(), newRole, adminId);
        return toUserResponse(user);
    }

    // ═══════════════════════════════════════════════════════════
    // DEMANDES DE RÔLE
    // ═══════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public PageResponse<RoleRequest> getRoleRequests(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<RoleRequest> requests = status != null
                ? roleRequestRepository.findByStatusOrderByCreatedAtDesc(
                    RoleRequest.Status.valueOf(status), pageable)
                : roleRequestRepository.findAllByOrderByCreatedAtDesc(pageable);
        return PageResponse.from(requests);
    }

    @Transactional(readOnly = true)
    public List<RoleRequest> getMyRoleRequests(String userId) {
        return roleRequestRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public RoleRequest approveRoleRequest(String requestId, String adminId, String comment) {
        RoleRequest request = roleRequestRepository.findById(requestId)
                .orElseThrow(() -> new InvalidOperationException("Demande introuvable"));
        request.approve(adminId, comment);
        roleRequestRepository.save(request);

        User user = userRepository.findByIdAndDeletedAtIsNull(request.getUserId())
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable"));
        user.setRole(request.getRequestedRole());
        userRepository.save(user);

        notificationPort.notify(request.getUserId(), "SYSTEM", "Demande de rôle approuvée",
                "Votre demande pour devenir " + request.getRequestedRole() + " a été approuvée.");
        emailPort.sendGeneral(user.getEmail(), "Votre demande de rôle ShareWay a été approuvée",
                "Bonjour " + user.getFirstName() + ",\n\nVotre demande pour devenir " +
                        request.getRequestedRole() + " a été approuvée. " +
                        "Vous pouvez maintenant " + (request.getRequestedRole() == User.UserRole.PASSENGER ? "réserver des trajets" : "proposer des trajets") + ".");
        auditPort.log("ROLE_REQUEST_APPROVED", "RoleRequest", requestId, null, request.getRequestedRole().name(), adminId);
        return request;
    }

    public RoleRequest rejectRoleRequest(String requestId, String adminId, String comment) {
        RoleRequest request = roleRequestRepository.findById(requestId)
                .orElseThrow(() -> new InvalidOperationException("Demande introuvable"));
        request.reject(adminId, comment);
        roleRequestRepository.save(request);

        User user = userRepository.findByIdAndDeletedAtIsNull(request.getUserId())
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable"));
        notificationPort.notify(request.getUserId(), "SYSTEM", "Demande de rôle rejetée",
                "Votre demande pour devenir " + request.getRequestedRole() + " a été rejetée." +
                        (comment != null ? " Raison: " + comment : ""));
        emailPort.sendGeneral(user.getEmail(), "Votre demande de rôle ShareWay n'a pas été approuvée",
                "Bonjour " + user.getFirstName() + ",\n\nVotre demande pour devenir " +
                        request.getRequestedRole() + " n'a pas été approuvée." +
                        (comment != null ? "\nRaison: " + comment : ""));
        auditPort.log("ROLE_REQUEST_REJECTED", "RoleRequest", requestId, null, comment, adminId);
        return request;
    }

    // ═══════════════════════════════════════════════════════════
    // GESTION DES VOYAGES
    // ═══════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public PageResponse<TripResponse> getAllTrips(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("departureTime").descending());
        Page<Trip> trips = status != null
                ? tripRepository.findByStatus(Trip.TripStatus.valueOf(status), pageable)
                : tripRepository.findAll(pageable);
        return PageResponse.from(trips.map(this::toTripResponse));
    }

    public TripResponse approveTrip(String tripId, String adminId) {
        Trip trip = tripRepository.findByIdAndDeletedAtIsNull(tripId)
                .orElseThrow(() -> new TripNotFoundException("Trajet introuvable: " + tripId));
        trip.setStatus(Trip.TripStatus.OPEN);
        tripRepository.save(trip);
        auditPort.log("TRIP_APPROVED", "Trip", tripId, null, null, adminId);
        return toTripResponse(trip);
    }

    public TripResponse rejectTrip(String tripId, String adminId, String reason) {
        Trip trip = tripRepository.findByIdAndDeletedAtIsNull(tripId)
                .orElseThrow(() -> new TripNotFoundException("Trajet introuvable: " + tripId));
        trip.setStatus(Trip.TripStatus.REJECTED);
        tripRepository.save(trip);
        notificationPort.notify(trip.getDriver().getId(), "SYSTEM", "Trajet rejeté",
                "Votre trajet " + trip.getDepartureCity() + " → " + trip.getArrivalCity() + " a été rejeté." +
                        (reason != null ? " Raison: " + reason : ""));
        emailPort.sendGeneral(trip.getDriver().getEmail(), "Trajet rejeté - ShareWay",
                "Bonjour " + trip.getDriver().getFirstName() + ",\n\nVotre trajet " +
                        trip.getDepartureCity() + " → " + trip.getArrivalCity() +
                        " a été rejeté par un administrateur." +
                        (reason != null ? "\nRaison: " + reason : ""));
        auditPort.log("TRIP_REJECTED", "Trip", tripId, null, reason, adminId);
        return toTripResponse(trip);
    }

    public TripResponse suspendTrip(String tripId, String adminId, String reason) {
        Trip trip = tripRepository.findByIdAndDeletedAtIsNull(tripId)
                .orElseThrow(() -> new TripNotFoundException("Trajet introuvable: " + tripId));
        trip.setStatus(Trip.TripStatus.SUSPENDED);
        tripRepository.save(trip);
        notificationPort.notify(trip.getDriver().getId(), "SYSTEM", "Trajet suspendu",
                "Votre trajet " + trip.getDepartureCity() + " → " + trip.getArrivalCity() + " a été suspendu." +
                        (reason != null ? " Raison: " + reason : ""));
        emailPort.sendGeneral(trip.getDriver().getEmail(), "Trajet suspendu - ShareWay",
                "Bonjour " + trip.getDriver().getFirstName() + ",\n\nVotre trajet " +
                        trip.getDepartureCity() + " → " + trip.getArrivalCity() +
                        " a été suspendu par un administrateur." +
                        (reason != null ? "\nRaison: " + reason : ""));
        auditPort.log("TRIP_SUSPENDED", "Trip", tripId, null, reason, adminId);
        return toTripResponse(trip);
    }

    public TripResponse reactivateTrip(String tripId, String adminId) {
        Trip trip = tripRepository.findByIdAndDeletedAtIsNull(tripId)
                .orElseThrow(() -> new TripNotFoundException("Trajet introuvable: " + tripId));
        trip.setStatus(Trip.TripStatus.OPEN);
        tripRepository.save(trip);
        notificationPort.notify(trip.getDriver().getId(), "SYSTEM", "Trajet réactivé",
                "Votre trajet " + trip.getDepartureCity() + " → " + trip.getArrivalCity() + " a été réactivé.");
        auditPort.log("TRIP_REACTIVATED", "Trip", tripId, null, null, adminId);
        return toTripResponse(trip);
    }

    public void softDeleteTrip(String tripId, String adminId, String reason) {
        Trip trip = tripRepository.findByIdAndDeletedAtIsNull(tripId)
                .orElseThrow(() -> new TripNotFoundException("Trajet introuvable: " + tripId));
        trip.setStatus(Trip.TripStatus.CANCELLED);
        trip.softDelete(adminId);
        tripRepository.save(trip);
        notificationPort.notify(trip.getDriver().getId(), "SYSTEM", "Trajet supprimé",
                "Votre trajet " + trip.getDepartureCity() + " → " + trip.getArrivalCity() + " a été supprimé par un administrateur." +
                        (reason != null ? " Raison: " + reason : ""));
        emailPort.sendGeneral(trip.getDriver().getEmail(), "Trajet supprimé - ShareWay",
                "Bonjour " + trip.getDriver().getFirstName() + ",\n\nVotre trajet " +
                        trip.getDepartureCity() + " → " + trip.getArrivalCity() +
                        " a été supprimé par un administrateur." +
                        (reason != null ? "\nRaison: " + reason : ""));
        auditPort.log("TRIP_DELETED", "Trip", tripId, null, reason, adminId);
    }

    @Transactional(readOnly = true)
    public TripResponse getTripDetail(String tripId) {
        Trip trip = tripRepository.findByIdAndDeletedAtIsNull(tripId)
                .orElseThrow(() -> new TripNotFoundException("Trajet introuvable: " + tripId));
        return toTripResponse(trip);
    }

    private TripResponse toTripResponse(Trip t) {
        TripUserResponse driverResp = null;
        if (t.getDriver() != null) {
            var d = t.getDriver();
            driverResp = TripUserResponse.builder()
                    .id(d.getId())
                    .firstName(d.getFirstName())
                    .lastName(d.getLastName())
                    .phone(d.getPhone())
                    .avatarUrl(d.getAvatarUrl())
                    .build();
        }
        return TripResponse.builder()
                .id(t.getId())
                .departureCity(t.getDepartureCity()).arrivalCity(t.getArrivalCity())
                .departureAddress(t.getDepartureAddress()).arrivalAddress(t.getArrivalAddress())
                .departureTime(t.getDepartureTime()).arrivalTime(t.getArrivalTime())
                .totalSeats(t.getTotalSeats()).availableSeats(t.getAvailableSeats())
                .pricePerSeat(t.getPricePerSeat()).currency(t.getCurrency().name())
                .description(t.getDescription()).status(t.getStatus().name())
                .recurring(t.isRecurring()).frequency(t.getFrequency() != null ? t.getFrequency().name() : null)
                .createdAt(t.getCreatedAt())
                .driver(driverResp)
                .build();
    }

    @Transactional
    public void updateSystemSetting(String key, String value) {
        var setting = systemSettingRepository.findByKey(key)
                .orElse(com.shareway.domain.model.SystemSetting.builder().key(key).build());
        setting.setValue(value);
        systemSettingRepository.save(setting);
    }
}
