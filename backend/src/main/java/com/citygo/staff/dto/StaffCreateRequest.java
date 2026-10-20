package com.citygo.staff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record StaffCreateRequest(
        @NotBlank @Size(min = 3, max = 32)
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "仅允许字母数字下划线")
        String username,

        @NotBlank @Size(min = 6, max = 64)
        String password,

        @Size(max = 64) String nickname,

        @NotBlank @Pattern(regexp = "^(OPERATOR|ADMIN)$", message = "角色仅允许 OPERATOR 或 ADMIN")
        String role
) {
}
