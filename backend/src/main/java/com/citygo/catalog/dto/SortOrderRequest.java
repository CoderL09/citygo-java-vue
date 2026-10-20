package com.citygo.catalog.dto;

import jakarta.validation.constraints.NotNull;

public record SortOrderRequest(@NotNull Integer sortOrder) {
}
