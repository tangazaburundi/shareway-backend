package com.shareway.domain.repository;

import com.shareway.domain.model.UserBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserBlacklistRepository extends JpaRepository<UserBlacklist, String> {
    List<UserBlacklist> findByUserId(String userId);

    boolean existsByUserIdAndBlockedUserId(String userId, String blockedUserId);

    void deleteByUserIdAndBlockedUserId(String userId, String blockedUserId);
}
