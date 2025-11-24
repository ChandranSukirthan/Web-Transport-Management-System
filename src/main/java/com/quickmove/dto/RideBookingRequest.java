package com.quickmove.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideBookingRequest {
    @NotBlank(message = "userId is required")
    @Size(max = 64, message = "userId too long")
    private String userId;

    @NotBlank(message = "vehicleType is required")
    @Size(max = 16, message = "vehicleType too long")
    private String vehicleType; // bike, car, auto

    @NotBlank(message = "pickupLocation is required")
    @Size(max = 255, message = "pickupLocation too long")
    private String pickupLocation;

    @Size(max = 255, message = "dropoffLocation too long")
    private String dropoffLocation; // optional

    @PositiveOrZero(message = "fare must be >= 0")
    private Double fare; // optional, server can compute if null
}