package com.citygo.auth.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * users 表实体。角色：USER | OPERATOR | ADMIN。
 */
@Data
public class User {

    private Long id;
    private String username;
    private String passwordHash;
    private String nickname;
    private String phone;
    private String role;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
