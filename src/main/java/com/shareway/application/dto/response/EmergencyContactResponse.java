package com.shareway.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencyContactResponse {

    private String id;
    private String name;
    private String phone;
    private String relationship;
    private boolean active;
    private String createdAt;
}
