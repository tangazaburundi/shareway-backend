package com.shareway.application.usecase;

import com.shareway.domain.exception.InvalidOperationException;
import com.shareway.domain.exception.UserNotFoundException;
import com.shareway.domain.model.Referral;
import com.shareway.domain.model.User;
import com.shareway.domain.repository.ReferralRepository;
import com.shareway.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ReferralUseCase {

    private final ReferralRepository referralRepository;
    private final UserRepository userRepository;

    public String generateReferralCode(String userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable"));

        Referral referral = Referral.builder()
                .referrer(user)
                .build();
        referralRepository.save(referral);
        return referral.getReferralCode();
    }

    public void applyReferralCode(String code, String newUserId) {
        Referral referral = referralRepository.findByReferralCode(code)
                .orElseThrow(() -> new InvalidOperationException("Code de parrainage invalide"));

        if (referral.getStatus() != Referral.ReferralStatus.PENDING) {
            throw new InvalidOperationException("Ce code de parrainage a déjà été utilisé");
        }

        User newUser = userRepository.findByIdAndDeletedAtIsNull(newUserId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable"));

        if (referral.getReferrer().getId().equals(newUserId)) {
            throw new InvalidOperationException("Vous ne pouvez pas vous parrainer vous-même");
        }

        referral.complete(newUserId);
        referralRepository.save(referral);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getReferralStats(String userId) {
        userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable"));

        long count = referralRepository.countByReferrerIdAndStatus(userId, Referral.ReferralStatus.COMPLETED);
        BigDecimal totalRewards = referralRepository.totalRewardsByReferrerId(userId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("referralCount", count);
        stats.put("totalRewards", totalRewards);
        stats.put("rewardCurrency", "EUR");
        return stats;
    }
}
