package com.citygo.config;

import com.citygo.auth.security.JwtAuthenticationFilter;
import com.citygo.auth.security.RestAccessDeniedHandler;
import com.citygo.auth.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 权限矩阵（实施书 §4，固定三角色）。前端路由守卫只是体验，后端以此为准。
 * 规则按声明顺序，先具体后宽泛：
 * - 公开：注册/登录、分类/活动/场次浏览、图片文件、接单开关查询、health
 * - /api/admin/staff/**：仅 ADMIN
 * - /api/admin/**：OPERATOR、ADMIN
 * - /api/user/**：任何已认证账号（本人资料）
 * - 其余：需认证
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/payment-callbacks/mock", "/api/payment-callbacks/mock-refund").permitAll()
                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers("/api/categories", "/api/categories/**").permitAll()
                        .requestMatchers("/api/activities", "/api/activities/**").permitAll()
                        .requestMatchers("/api/files/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/settings/**").permitAll()
                        .requestMatchers("/ws").permitAll()
                        .requestMatchers("/actuator/health", "/error").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/admin/settings/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/packages", "/api/packages/**").permitAll()
                        .requestMatchers("/api/contacts", "/api/contacts/**", "/api/cart", "/api/cart/**", "/api/orders", "/api/orders/**").hasRole("USER")
                        .requestMatchers("/api/admin/audit", "/api/admin/audit/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/staff/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/**").hasAnyRole("OPERATOR", "ADMIN")
                        .requestMatchers("/api/user/**").authenticated()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
