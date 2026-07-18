package com.shareway.domain.repository;

import com.shareway.domain.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    boolean existsByEmail(String email);

    Optional<User> findByEmailVerifyToken(String token);

    Optional<User> findByIdAndDeletedAtIsNull(String id);

    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL ORDER BY u.createdAt DESC")
    Page<User> findAllActive(Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL AND u.blocked = true")
    List<User> findAllBlocked();

    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL AND u.identityVerified = false " +
            "AND EXISTS (SELECT d FROM UserDocument d WHERE d.user = u AND d.status = 'PENDING')")
    List<User> findUsersWithPendingDocuments();

    @Query("SELECT COUNT(u) FROM User u WHERE u.deletedAt IS NULL AND u.active = true")
    long countActiveUsers();

    @Query("SELECT COUNT(u) FROM User u WHERE u.deletedAt IS NULL AND (u.role = 'DRIVER' OR u.role = 'BOTH')")
    long countDrivers();

    @Query("SELECT COUNT(u) FROM User u WHERE FUNCTION('DATE', u.createdAt) = CURRENT_DATE")
    long countNewUsersToday();

    @Query(value = "SELECT * FROM users WHERE deleted_at IS NULL AND " +
            "MATCH(first_name, last_name, email) AGAINST(:query IN BOOLEAN MODE) LIMIT 20",
            nativeQuery = true)
    List<User> searchUsers(@Param("query") String query);

    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = CURRENT_TIMESTAMP WHERE u.id = :id")
    void updateLastLogin(@Param("id") String id);

}
