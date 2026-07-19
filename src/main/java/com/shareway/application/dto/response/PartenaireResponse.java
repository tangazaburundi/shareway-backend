package com.shareway.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartenaireResponse {
    private String id;
    private String nom;
    private String imageUrl;
    private String lienUrl;
    private boolean actif;
    private int sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
