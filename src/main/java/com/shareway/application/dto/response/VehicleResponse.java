package com.shareway.application.dto.response;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class VehicleResponse { private String id,brand,model,color,licensePlate,photoUrl; private Short year; }
