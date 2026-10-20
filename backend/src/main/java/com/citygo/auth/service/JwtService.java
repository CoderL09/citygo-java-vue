package com.citygo.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 签发与校验（jjwt 0.12）。
 * 短有效期、前端内存保存；退出撤销（jti → Redis）在 P4 接入，
 * 当前阶段退出后 token 在有效期内仍可用，属已知限制。
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long ttlSeconds;
    private final java.time.Clock clock;

    public JwtService(@Value("${citygo.jwt.secret}") String secret,
                      @Value("${citygo.jwt.ttl-seconds:1800}") long ttlSeconds, java.time.Clock clock) {
        this.clock=clock;
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("CITYGO_JWT_SECRET 必须至少 32 字节（HS256 要求）");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlSeconds = ttlSeconds;
    }

    public record TokenResult(String token, Instant expiresAt) {
    }

    public TokenResult issue(Long userId, String username, String role) {
        Instant now = clock.instant();
        Instant expiresAt = now.plusSeconds(ttlSeconds);
        String token = Jwts.builder()
                .subject(username)
                .claim("uid", userId)
                .claim("role", role)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
        return new TokenResult(token, expiresAt);
    }

    /**
     * 解析并校验签名与有效期；失败抛 JwtException（调用方按未认证处理）。
     */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).clock(() -> Date.from(clock.instant())).build().parseSignedClaims(token).getPayload();
    }
}
