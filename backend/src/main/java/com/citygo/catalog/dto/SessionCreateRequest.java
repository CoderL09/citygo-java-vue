package com.citygo.catalog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record SessionCreateRequest(
        @NotNull LocalDateTime startTime,
        @NotNull LocalDateTime endTime,
        @NotNull LocalDateTime bookingDeadline,
        @NotNull @Min(0) Integer priceCents,
        @NotNull @Min(1) Integer capacity
) {
}
