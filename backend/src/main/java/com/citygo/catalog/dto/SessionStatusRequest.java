package com.citygo.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SessionStatusRequest(
        @NotBlank @Pattern(regexp = "^(ON_SALE|OFF_SALE)$", message = "状态仅允许 ON_SALE 或 OFF_SALE")
        String status
) {
}
