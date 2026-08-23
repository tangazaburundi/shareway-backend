package com.shareway.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoundPreferenceResponse {
    private String rideRequestSound;
    private String rideAcceptedSound;
    private String rideCancelledSound;
    private String rideCompletedSound;
    private String messageSound;
    private String sosSound;
    private BigDecimal notificationVolume;
}
