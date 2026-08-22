package com.shareway.domain.repository;

import com.shareway.domain.model.SmsConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SmsConfigRepository extends JpaRepository<SmsConfig, String> {

    Optional<SmsConfig> findTopByOrderByIdAsc();

    Optional<SmsConfig> findByProvider(SmsConfig.SmsProvider provider);
}
