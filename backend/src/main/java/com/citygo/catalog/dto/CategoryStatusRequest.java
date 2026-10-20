package com.citygo.catalog.dto;

import jakarta.validation.constraints.NotNull;

public record CategoryStatusRequest(@NotNull Boolean status) {
}
