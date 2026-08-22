package com.shareway.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideRatingResponse {

    private String id;
    private String rideRequestId;
    private String fromUserId;
    private String fromUserFirstName;
    private String fromUserLastName;
    private String fromUserAvatarUrl;
    private String toUserId;
    private int rating;
    private String comment;
    private String type;
    private String createdAt;
}
