package com.citygo.auth.dto;

import java.time.Instant;

/**
 * 登录/注册成功响应：token 由前端存内存（刷新需重新登录，首版限制）。
 */
public record LoginResponse(String token, Instant expiresAt, UserView user) {
}
