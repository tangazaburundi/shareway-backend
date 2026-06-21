package com.shareway.application.usecase;

import com.shareway.application.dto.response.UserResponse;
import com.shareway.infrastructure.adapter.audit.domain.exception.InvalidOperationException;
import com.shareway.infrastructure.adapter.audit.domain.exception.ResourceAlreadyExistsException;
import com.shareway.infrastructure.adapter.audit.domain.exception.UserNotFoundException;
import com.shareway.infrastructure.adapter.audit.domain.model.UserBlacklist;
import com.shareway.infrastructure.adapter.audit.domain.model.UserFavorite;
import com.shareway.infrastructure.adapter.audit.domain.repository.UserBlacklistRepository;
import com.shareway.infrastructure.adapter.audit.domain.repository.UserFavoriteRepository;
import com.shareway.infrastructure.adapter.audit.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FavoritesUseCase {

    private final UserFavoriteRepository favoriteRepository;
    private final UserBlacklistRepository blacklistRepository;
    private final UserRepository userRepository;

    public void addFavorite(String userId, String favoriteUserId) {
        if (userId.equals(favoriteUserId))
            throw new InvalidOperationException("Cannot add yourself as favorite");
        userRepository.findByIdAndDeletedAtIsNull(favoriteUserId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        if (favoriteRepository.existsByUserIdAndFavoriteUserId(userId, favoriteUserId))
            throw new ResourceAlreadyExistsException("Already in favorites");
        UserFavorite fav = UserFavorite.builder()
                .userId(userId).favoriteUserId(favoriteUserId).build();
        favoriteRepository.save(fav);
    }

    public void removeFavorite(String userId, String favoriteUserId) {
        favoriteRepository.deleteByUserIdAndFavoriteUserId(userId, favoriteUserId);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getFavorites(String userId) {
        return favoriteRepository.findByUserId(userId).stream()
                .map(fav -> userRepository.findByIdAndDeletedAtIsNull(fav.getFavoriteUserId()).orElse(null))
                .filter(u -> u != null)
                .map(u -> UserResponse.builder()
                        .id(u.getId()).firstName(u.getFirstName()).lastName(u.getLastName())
                        .avatarUrl(u.getAvatarUrl()).rating(u.getRating()).reviewCount(u.getReviewCount())
                        .identityVerified(u.isIdentityVerified()).build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean isFavorite(String userId, String targetId) {
        return favoriteRepository.existsByUserIdAndFavoriteUserId(userId, targetId);
    }

    // ===== BLACKLIST =====
    public void addToBlacklist(String userId, String blockedUserId, String reason) {
        if (userId.equals(blockedUserId))
            throw new InvalidOperationException("Cannot blacklist yourself");
        if (blacklistRepository.existsByUserIdAndBlockedUserId(userId, blockedUserId))
            throw new ResourceAlreadyExistsException("User already blacklisted");
        UserBlacklist entry = UserBlacklist.builder()
                .userId(userId).blockedUserId(blockedUserId).reason(reason).build();
        blacklistRepository.save(entry);
    }

    public void removeFromBlacklist(String userId, String blockedUserId) {
        blacklistRepository.deleteByUserIdAndBlockedUserId(userId, blockedUserId);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getBlacklist(String userId) {
        return blacklistRepository.findByUserId(userId).stream()
                .map(b -> userRepository.findByIdAndDeletedAtIsNull(b.getBlockedUserId()).orElse(null))
                .filter(u -> u != null)
                .map(u -> UserResponse.builder()
                        .id(u.getId()).firstName(u.getFirstName()).lastName(u.getLastName())
                        .avatarUrl(u.getAvatarUrl()).build())
                .collect(Collectors.toList());
    }
}
