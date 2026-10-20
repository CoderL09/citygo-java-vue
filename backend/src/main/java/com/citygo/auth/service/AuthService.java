package com.citygo.auth.service;

import com.citygo.auth.dto.LoginRequest;
import com.citygo.auth.dto.LoginResponse;
import com.citygo.auth.dto.RegisterRequest;
import com.citygo.auth.dto.UpdateProfileRequest;
import com.citygo.auth.dto.UserView;
import com.citygo.auth.entity.User;
import com.citygo.auth.mapper.UserMapper;
import com.citygo.auth.security.LoginUser;
import com.citygo.common.api.ErrorCode;
import com.citygo.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    public static final String ROLE_USER = "USER";

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Clock clock;

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (userMapper.findByUsername(request.username()) != null) {
            throw new BizException(ErrorCode.CONFLICT, "用户名已存在");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        User user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname());
        user.setPhone(request.phone());
        user.setRole(ROLE_USER); // 注册只能得到 USER 角色
        user.setEnabled(true);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);
        return issueLogin(user);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userMapper.findByUsername(request.username());
        // 统一提示避免用户名枚举
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BizException(ErrorCode.BAD_CREDENTIALS);
        }
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new BizException(ErrorCode.USER_DISABLED);
        }
        return issueLogin(user);
    }

    public UserView me(LoginUser principal) {
        User user = userMapper.findById(principal.id());
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return toView(user);
    }

    @Transactional
    public UserView updateProfile(LoginUser principal, UpdateProfileRequest request) {
        userMapper.updateProfile(principal.id(), request.nickname(), request.phone(), LocalDateTime.now(clock));
        return me(principal);
    }

    private LoginResponse issueLogin(User user) {
        JwtService.TokenResult token = jwtService.issue(user.getId(), user.getUsername(), user.getRole());
        return new LoginResponse(token.token(), token.expiresAt(), toView(user));
    }

    public static UserView toView(User user) {
        return new UserView(user.getId(), user.getUsername(), user.getNickname(), user.getPhone(),
                user.getRole(), user.getEnabled());
    }
}
