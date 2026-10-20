package com.citygo.catalog.dto;

import jakarta.validation.constraints.NotNull;

public record ImageRefRequest(@NotNull Long fileObjectId, Integer sortOrder) {
}
