package com.citygo.auth.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 64) String nickname,
        @Size(max = 20) String phone
) {
}
