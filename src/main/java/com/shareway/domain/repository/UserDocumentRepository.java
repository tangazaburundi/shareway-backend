package com.shareway.domain.repository;

import com.shareway.domain.model.UserDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserDocumentRepository extends JpaRepository<UserDocument, String> {
    List<UserDocument> findByUserIdAndDeletedAtIsNull(String userId);

    List<UserDocument> findByStatus(UserDocument.DocumentStatus status);
}
