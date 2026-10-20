package com.citygo.auth.security;

import com.citygo.common.api.ApiResponse;
import com.citygo.common.api.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 未认证统一 401 JSON；区分账号禁用（40101）与一般未登录（40100）。
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        Object attr = request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTR);
        int code = attr instanceof Integer i ? i : ErrorCode.UNAUTHORIZED.getCode();
        ErrorCode errorCode = switch(code){case 40101->ErrorCode.USER_DISABLED;case 40103->ErrorCode.SESSION_EXPIRED;case 50300->ErrorCode.SERVICE_UNAVAILABLE;default->ErrorCode.UNAUTHORIZED;};
        response.setStatus(code==50300?HttpServletResponse.SC_SERVICE_UNAVAILABLE:HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.error(errorCode.getCode(), errorCode.getDefaultMessage())));
    }
}
