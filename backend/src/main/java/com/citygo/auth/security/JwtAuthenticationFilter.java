package com.citygo.auth.security;

import com.citygo.auth.entity.User;
import com.citygo.auth.mapper.UserMapper;
import com.citygo.auth.service.JwtService;
import com.citygo.common.api.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 解析 Bearer token，按 userId 实时查库校验启停与角色（角色/禁用即时生效）。
 * 认证失败不在此处直接响应，而是置 request 属性，由 EntryPoint 统一输出 JSON。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_ERROR_ATTR = "citygo.authError";

    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final com.citygo.auth.service.TokenRevocationService revocations;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtService.parse(token);
                if(revocations.revoked(claims.getId()))throw new com.citygo.common.exception.BizException(ErrorCode.SESSION_EXPIRED);
                Long uid = claims.get("uid", Long.class);
                User user = userMapper.findById(uid);
                if (user == null) {
                    request.setAttribute(AUTH_ERROR_ATTR, ErrorCode.UNAUTHORIZED.getCode());
                } else if (!Boolean.TRUE.equals(user.getEnabled())) {
                    // 禁用即时生效：即使 token 有效也拒绝
                    request.setAttribute(AUTH_ERROR_ATTR, ErrorCode.USER_DISABLED.getCode());
                } else {
                    LoginUser principal = new LoginUser(user.getId(), user.getUsername(), user.getRole(), user.getNickname());
                    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
                    var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (com.citygo.common.exception.BizException e) {
                request.setAttribute(AUTH_ERROR_ATTR,e.getErrorCode().getCode());
            } catch (JwtException | IllegalArgumentException e) {
                // 签名/过期/格式错误 → 未认证
                log.debug("jwt parse failed: {}", e.getMessage());
            }
        }
        chain.doFilter(request, response);
    }
}
