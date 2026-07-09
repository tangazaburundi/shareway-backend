package com.shareway.domain.repository;

import com.shareway.domain.model.UserFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserFavoriteRepository extends JpaRepository<UserFavorite, String> {
    List<UserFavorite> findByUserId(String userId);

    boolean existsByUserIdAndFavoriteUserId(String userId, String favoriteUserId);

    void deleteByUserIdAndFavoriteUserId(String userId, String favoriteUserId);
}
