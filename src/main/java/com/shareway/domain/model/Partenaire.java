package com.shareway.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "partenaires")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Partenaire {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 200)
    private String nom;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "lien_url", length = 500)
    private String lienUrl;

    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public static Partenaire create(String nom, String imageUrl, String lienUrl, int sortOrder) {
        return Partenaire.builder()
                .nom(nom)
                .imageUrl(imageUrl)
                .lienUrl(lienUrl)
                .actif(true)
                .sortOrder(sortOrder)
                .build();
    }

    public void update(String nom, String imageUrl, String lienUrl, Boolean actif, Integer sortOrder) {
        if (nom != null) this.nom = nom;
        if (imageUrl != null) this.imageUrl = imageUrl;
        if (lienUrl != null) this.lienUrl = lienUrl;
        if (actif != null) this.actif = actif;
        if (sortOrder != null) this.sortOrder = sortOrder;
    }
}
