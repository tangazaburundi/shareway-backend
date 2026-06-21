package com.shareway.infrastructure.adapter.specification;

import com.shareway.infrastructure.adapter.audit.domain.model.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class UserSpecifications {

    public static Specification<User> searchActiveUsers(String search) {
        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // ✅ filtre obligatoire : utilisateurs actifs
            predicates.add(cb.equal(root.get("active"), true));

            // ✅ filtre search (nom / prénom / email)
            if (search != null && !search.isBlank()) {

                String like = "%" + search.toLowerCase() + "%";

                Predicate searchPredicate = cb.or(
                        cb.like(cb.lower(root.get("firstName")), like),
                        cb.like(cb.lower(root.get("lastName")), like),
                        cb.like(cb.lower(root.get("email")), like)
                );

                predicates.add(searchPredicate);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
