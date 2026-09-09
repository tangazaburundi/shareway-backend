package com.shareway.application.dto.request;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
@Data @NoArgsConstructor @AllArgsConstructor
public class FuelEntryRequest {
    @NotNull private LocalDate refuelDate;
    @NotNull @DecimalMin(value = "0.001") private BigDecimal liters;
    @NotNull @DecimalMin(value = "0.01") private BigDecimal pricePerLiter;
    @Size(max = 3) private String currency;
    @DecimalMin(value = "0") private BigDecimal odometerKm;
    @Size(max = 100) private String stationName;
    @Size(max = 500) private String notes;
}