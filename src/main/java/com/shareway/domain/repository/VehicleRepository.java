package com.shareway.domain.repository;

import com.shareway.domain.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, String> {

    /**
     * Retourne le véhicule actif d'un utilisateur (PUT /users/me/vehicle)
     */
    Optional<Vehicle> findFirstByUserIdAndActiveTrue(String userId);

    /**
     * Tous les véhicules d'un utilisateur
     */
    List<Vehicle> findByUserId(String userId);
}
