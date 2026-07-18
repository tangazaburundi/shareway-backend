package com.shareway.domain.repository;

import com.shareway.domain.model.Visitor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface VisitorRepository extends JpaRepository<Visitor, String> {

    String ADMIN_EMAIL = "sharewaybdi@gmail.com";

    long countByVisitedAtBetweenAndUserEmailNot(LocalDateTime from, LocalDateTime to, String userEmail);

    @Query("SELECT COUNT(v) FROM Visitor v WHERE v.visitedAt BETWEEN :from AND :to AND (v.userEmail IS NULL OR v.userEmail <> :email)")
    long countBetweenExcluding(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, @Param("email") String email);

    long countByUserIdIsNotNullAndUserEmailNot(String userEmail);

    @Query("SELECT v.country, COUNT(v) FROM Visitor v WHERE v.country IS NOT NULL AND (v.userEmail IS NULL OR v.userEmail <> :email) GROUP BY v.country ORDER BY COUNT(v) DESC")
    List<Object[]> countByCountryExcluding(@Param("email") String email);

    @Query("SELECT v.city, COUNT(v) FROM Visitor v WHERE v.city IS NOT NULL AND (v.userEmail IS NULL OR v.userEmail <> :email) GROUP BY v.city ORDER BY COUNT(v) DESC")
    List<Object[]> countByCityExcluding(@Param("email") String email);

    long countByAcceptedCookiesTrueAndUserEmailNot(String userEmail);

    long countByAcceptedCookiesFalseAndUserEmailNot(String userEmail);

    @Query("SELECT COUNT(DISTINCT v.anonymousId) FROM Visitor v WHERE v.anonymousId IS NOT NULL AND v.visitedAt >= :since AND (v.userEmail IS NULL OR v.userEmail <> :email)")
    long countDistinctAnonymousSinceExcluding(@Param("since") LocalDateTime since, @Param("email") String email);

    @Query("SELECT COUNT(DISTINCT v.userId) FROM Visitor v WHERE v.userId IS NOT NULL AND v.visitedAt >= :since AND (v.userEmail IS NULL OR v.userEmail <> :email)")
    long countDistinctUsersSinceExcluding(@Param("since") LocalDateTime since, @Param("email") String email);

    @Query("SELECT COUNT(v) FROM Visitor v WHERE (v.userEmail IS NULL OR v.userEmail <> :email)")
    long countExcluding(@Param("email") String email);

    @Query("SELECT v FROM Visitor v WHERE " +
           "(v.userEmail IS NULL OR v.userEmail <> :email) AND " +
           "(:search IS NULL OR LOWER(v.userName) LIKE LOWER(CONCAT('%',:search,'%')) OR LOWER(v.userEmail) LIKE LOWER(CONCAT('%',:search,'%')) OR LOWER(v.anonymousId) LIKE LOWER(CONCAT('%',:search,'%'))) AND " +
           "(:country IS NULL OR v.country = :country) AND " +
           "(:cookiesAccepted IS NULL OR v.acceptedCookies = :cookiesAccepted) " +
           "ORDER BY v.visitedAt DESC")
    Page<Visitor> searchExcluding(@Param("email") String email,
                                  @Param("search") String search,
                                  @Param("country") String country,
                                  @Param("cookiesAccepted") Boolean cookiesAccepted,
                                  Pageable pageable);
}
