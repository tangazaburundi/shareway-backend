package com.shareway.domain.repository;

import com.shareway.domain.model.PricingConfig;
import com.shareway.domain.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PricingConfigRepository extends JpaRepository<PricingConfig, String> {

    Optional<PricingConfig> findByCurrencyAndActiveTrue(Trip.Currency currency);

    Optional<PricingConfig> findByCurrency(Trip.Currency currency);
}
