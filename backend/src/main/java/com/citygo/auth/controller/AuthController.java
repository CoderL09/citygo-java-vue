package com.citygo.auth.controller;

import com.citygo.auth.dto.LoginRequest;
import com.citygo.auth.dto.LoginResponse;
import com.citygo.auth.dto.RegisterRequest;
import com.citygo.auth.dto.UpdateProfileRequest;
import com.citygo.auth.dto.UserView;
import com.citygo.auth.security.LoginUser;
import com.citygo.auth.service.AuthService;
import com.citygo.common.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final com.citygo.auth.service.TokenRevocationService revocations;

    @PostMapping("/api/auth/register")
    public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    @PostMapping("/api/auth/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    /**
     * P1 无 Redis，退出只作前端清理；jti 撤销列表在 P4 接入（文档已标注限制）。
     */
    @PostMapping("/api/auth/logout")
    public ApiResponse<Void> logout(@org.springframework.web.bind.annotation.RequestHeader("Authorization")String bearer) {
        revocations.logout(bearer);
        return ApiResponse.ok();
    }

    @GetMapping("/api/auth/me")
    public ApiResponse<UserView> me(@AuthenticationPrincipal LoginUser principal) {
        return ApiResponse.ok(authService.me(principal));
    }

    @RequestMapping("/api/user")
    @RestController
    @RequiredArgsConstructor
    public static class ProfileController {

        private final AuthService authService;

        @PutMapping("/profile")
        public ApiResponse<UserView> updateProfile(@AuthenticationPrincipal LoginUser principal,
                                                   @Valid @RequestBody UpdateProfileRequest request) {
            return ApiResponse.ok(authService.updateProfile(principal, request));
        }
    }
}
