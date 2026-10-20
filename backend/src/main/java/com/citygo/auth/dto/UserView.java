package com.citygo.auth.dto;

/**
 * 对外用户视图：绝不携带密码摘要。
 */
public record UserView(Long id, String username, String nickname, String phone, String role, Boolean enabled) {
}
