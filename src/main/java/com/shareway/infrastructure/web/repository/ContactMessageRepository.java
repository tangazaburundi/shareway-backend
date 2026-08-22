package com.shareway.infrastructure.web.repository;

import com.shareway.infrastructure.web.entity.ContactMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, String> {
}
