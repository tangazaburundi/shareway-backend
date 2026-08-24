package com.shareway.domain.repository;

import com.shareway.domain.model.FuelEntry;
import com.shareway.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FuelEntryRepository extends JpaRepository<FuelEntry, String> {

    Optional<FuelEntry> findByIdAndDriver(String id, User driver);

    List<FuelEntry> findByDriverOrderByRefuelDateDesc(User driver);

    @Query("SELECT f FROM FuelEntry f WHERE f.driver = :driver AND f.refuelDate >= :startDate AND f.refuelDate < :endDate ORDER BY f.refuelDate DESC")
    List<FuelEntry> findByDriverAndDateRange(
            @Param("driver") User driver,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(f.liters), 0) FROM FuelEntry f WHERE f.driver = :driver AND f.refuelDate >= :startDate AND f.refuelDate < :endDate")
    java.math.BigDecimal sumLitersByDriverAndDateRange(
            @Param("driver") User driver,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(f.totalCost), 0) FROM FuelEntry f WHERE f.driver = :driver AND f.refuelDate >= :startDate AND f.refuelDate < :endDate")
    java.math.BigDecimal sumCostByDriverAndDateRange(
            @Param("driver") User driver,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(f.liters), 0) FROM FuelEntry f WHERE f.driver = :driver")
    java.math.BigDecimal sumAllLitersByDriver(@Param("driver") User driver);

    @Query("SELECT COALESCE(SUM(f.totalCost), 0) FROM FuelEntry f WHERE f.driver = :driver")
    java.math.BigDecimal sumAllCostByDriver(@Param("driver") User driver);
}
