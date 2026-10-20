package com.citygo.catalog.dto;
import jakarta.validation.constraints.*;
import java.util.List;
public record PackageRequest(@NotNull @Positive Long categoryId,
        @NotBlank @Size(max=128) String name,@Size(max=10000) String description,
        @NotNull @Min(0) Integer packagePriceCents,
        @NotEmpty @Size(max=10) List<@NotNull @Positive Long> sessionIds) {}
