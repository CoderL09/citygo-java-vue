package com.citygo.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ActivityCreateRequest(
        @NotNull Long categoryId,
        @NotBlank @Size(max = 128) String name,
        @Size(max = 2000) String description,
        @NotBlank @Size(max = 255) String address
) {
}
