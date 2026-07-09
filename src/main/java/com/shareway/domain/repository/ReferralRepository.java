package com.shareway.domain.repository;

import com.shareway.domain.model.Referral;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface ReferralRepository extends JpaRepository<Referral, String> {
    Optional<Referral> findByReferralCode(String referralCode);

    long countByReferrerIdAndStatus(String referrerId, Referral.ReferralStatus status);

    @Query("SELECT COALESCE(SUM(r.rewardAmount), 0) FROM Referral r WHERE r.referrer.id = :referrerId AND r.status = 'COMPLETED'")
    BigDecimal totalRewardsByReferrerId(@Param("referrerId") String referrerId);
}
