package com.citygo.staff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record StaffUpdateRequest(
        @Size(max = 64) String nickname,

        @NotBlank @Pattern(regexp = "^(OPERATOR|ADMIN)$", message = "角色仅允许 OPERATOR 或 ADMIN")
        String role
) {
}
