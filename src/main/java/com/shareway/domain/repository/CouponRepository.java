package com.shareway.domain.repository;

import com.shareway.domain.model.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, String> {
    Optional<Coupon> findByCodeAndActiveTrue(String code);

    Optional<Coupon> findByCode(String code);
}
