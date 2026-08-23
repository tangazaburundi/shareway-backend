package com.shareway.application.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class UpdateSoundPreferenceRequest {

    @Size(max = 50)
    private String rideRequestSound = "classic";

    @Size(max = 50)
    private String rideAcceptedSound = "success";

    @Size(max = 50)
    private String rideCancelledSound = "alert";

    @Size(max = 50)
    private String rideCompletedSound = "tada";

    @Size(max = 50)
    private String messageSound = "ping";

    @Size(max = 50)
    private String sosSound = "siren";

    @DecimalMin("0.00")
    @DecimalMax("1.00")
    private BigDecimal notificationVolume = new BigDecimal("0.30");
}
