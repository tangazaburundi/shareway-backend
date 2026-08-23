package com.shareway.domain.repository;

import com.shareway.domain.model.UserSoundPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserSoundPreferenceRepository extends JpaRepository<UserSoundPreference, String> {
    Optional<UserSoundPreference> findByUserId(String userId);
    void deleteByUserId(String userId);
}
