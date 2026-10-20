package com.citygo.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 注册请求。角色固定 USER：普通注册不得指定角色（实施书 §4）。
 */
public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 32)
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "仅允许字母数字下划线")
        String username,

        @NotBlank @Size(min = 6, max = 64)
        String password,

        @Size(max = 64) String nickname,

        @Size(max = 20) String phone
) {
}
