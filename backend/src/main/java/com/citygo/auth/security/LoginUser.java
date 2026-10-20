package com.citygo.auth.security;

/**
 * 已认证主体：每次请求由 JwtAuthenticationFilter 从数据库实时加载
 * （启停与角色变化即时生效，不信任 token 内的旧角色）。
 */
public record LoginUser(Long id, String username, String role, String nickname) {
}
