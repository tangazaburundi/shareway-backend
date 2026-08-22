package com.shareway.domain.repository;

import com.shareway.domain.model.PromoCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PromoCodeRepository extends JpaRepository<PromoCode, String> {

    Optional<PromoCode> findByCodeIgnoreCase(String code);

    @Query("SELECT p FROM PromoCode p WHERE p.code = :code AND p.active = true")
    Optional<PromoCode> findActiveByCode(String code);
}
