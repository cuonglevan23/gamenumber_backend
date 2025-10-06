package com.numbergame.gamenumber.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuessRequest {

    @NotNull(message = "Number is required")
    @Min(value = 1, message = "Number must be at least 1")
    @Max(value = 5, message = "Number must be at most 5")
    private Integer number;

    /**
     * Custom win probability for this guess (optional)
     * If not provided, uses default game.win-rate from configuration (5%)
     * Range: 0.01 (1%) to 1.0 (100%)
     */
    @DecimalMin(value = "0.01", message = "Win probability must be at least 0.01 (1%)")
    @DecimalMax(value = "1.0", message = "Win probability must be at most 1.0 (100%)")
    private Double winProbability;
}
