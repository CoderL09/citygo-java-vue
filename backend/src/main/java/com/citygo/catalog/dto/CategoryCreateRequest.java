package com.citygo.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryCreateRequest(
        @NotBlank @Pattern(regexp = "^(ACTIVITY|PACKAGE)$", message = "类型仅允许 ACTIVITY 或 PACKAGE")
        String categoryType,

        @NotBlank @Size(max = 64) String name,

        Integer sortOrder
) {
}
