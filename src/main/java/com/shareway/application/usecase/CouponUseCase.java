package com.shareway.application.usecase;

import com.shareway.domain.exception.InvalidOperationException;
import com.shareway.domain.model.Coupon;
import com.shareway.domain.repository.CouponRepository;
import com.shareway.domain.repository.CouponUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class CouponUseCase {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;

    @Transactional(readOnly = true)
    public CouponValidationResult validate(String code, String userId, BigDecimal tripAmount) {
        Coupon coupon = couponRepository.findByCodeAndActiveTrue(code)
                .orElseThrow(() -> new InvalidOperationException("Invalid or expired coupon code"));

        LocalDateTime now = LocalDateTime.now();
        if (coupon.getStartsAt() != null && now.isBefore(coupon.getStartsAt()))
            throw new InvalidOperationException("Coupon is not yet active");
        if (coupon.getExpiresAt() != null && now.isAfter(coupon.getExpiresAt()))
            throw new InvalidOperationException("Coupon has expired");
        if (coupon.getMaxUses() != null && coupon.getCurrentUses() >= coupon.getMaxUses())
            throw new InvalidOperationException("Coupon usage limit reached");

        long userUsages = couponUsageRepository.countByUserIdAndCouponId(userId, coupon.getId());
        if (userUsages >= coupon.getMaxUsesPerUser())
            throw new InvalidOperationException("You have already used this coupon");

        if (coupon.getMinTripAmount() != null && tripAmount.compareTo(coupon.getMinTripAmount()) < 0)
            throw new InvalidOperationException("Minimum trip amount is " + coupon.getMinTripAmount());

        BigDecimal discount;
        if (coupon.getDiscountType() == Coupon.DiscountType.PERCENT) {
            discount = tripAmount.multiply(coupon.getDiscountValue()).divide(BigDecimal.valueOf(100));
            if (coupon.getMaxDiscount() != null && discount.compareTo(coupon.getMaxDiscount()) > 0)
                discount = coupon.getMaxDiscount();
        } else {
            discount = coupon.getDiscountValue();
        }

        BigDecimal finalAmount = tripAmount.subtract(discount).max(BigDecimal.ZERO);
        return new CouponValidationResult(coupon.getId(), code, discount, finalAmount);
    }

    public record CouponValidationResult(
            String couponId, String code, BigDecimal discount, BigDecimal finalAmount) {
    }
}
