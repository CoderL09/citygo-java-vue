package com.citygo.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryUpdateRequest(
        @NotBlank @Size(max = 64) String name,
        Integer sortOrder
) {
}
