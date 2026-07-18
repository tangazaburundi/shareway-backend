package com.shareway.domain.repository;

import com.shareway.domain.model.NewsletterSubscriber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NewsletterSubscriberRepository extends JpaRepository<NewsletterSubscriber, String> {
    Optional<NewsletterSubscriber> findByEmail(String email);
    boolean existsByEmail(String email);
}
