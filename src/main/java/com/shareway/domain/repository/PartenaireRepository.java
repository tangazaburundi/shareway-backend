package com.shareway.domain.repository;

import com.shareway.domain.model.Partenaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartenaireRepository extends JpaRepository<Partenaire, String> {

    List<Partenaire> findByActifTrueOrderBySortOrderAsc();

    List<Partenaire> findAllByOrderBySortOrderAsc();
}
