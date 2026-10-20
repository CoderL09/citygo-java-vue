package com.citygo.staff.service;

import com.citygo.auth.dto.UserView;
import com.citygo.auth.entity.User;
import com.citygo.auth.mapper.UserMapper;
import com.citygo.auth.security.LoginUser;
import com.citygo.auth.service.AuthService;
import com.citygo.common.api.ErrorCode;
import com.citygo.common.api.PageResult;
import com.citygo.common.exception.BizException;
import com.citygo.staff.dto.ResetPasswordRequest;
import com.citygo.staff.dto.SetEnabledRequest;
import com.citygo.staff.dto.StaffCreateRequest;
import com.citygo.staff.dto.StaffUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 员工管理（仅 ADMIN）。启停与角色变更即时生效：JWT 过滤器每次请求实时查库。
 * 保护规则：不能修改/禁用自己；不能禁用或降级最后一个启用的 ADMIN，避免平台锁死。
 */
@Service
@RequiredArgsConstructor
public class StaffService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public PageResult<UserView> page(String keyword, int page, int pageSize) {
        long total = userMapper.countStaff(keyword);
        List<User> users = userMapper.pageStaff(keyword, (page - 1) * pageSize, pageSize);
        return PageResult.of(users.stream().map(AuthService::toView).toList(), total, page, pageSize);
    }

    @Transactional
    public UserView create(StaffCreateRequest request) {
        if (userMapper.findByUsername(request.username()) != null) {
            throw new BizException(ErrorCode.CONFLICT, "用户名已存在");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        User user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname());
        user.setRole(request.role());
        user.setEnabled(true);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);
        return AuthService.toView(user);
    }

    @Transactional
    public UserView update(LoginUser operator, Long id, StaffUpdateRequest request) {
        User target = requireStaff(id);
        if (target.getId().equals(operator.id()) && !target.getRole().equals(request.role())) {
            throw new BizException(ErrorCode.FORBIDDEN, "不能修改自己的角色");
        }
        if (target.getRole().equals("ADMIN") && !"ADMIN".equals(request.role())
                && userMapper.countEnabledAdmins() <= 1) {
            throw new BizException(ErrorCode.CONFLICT, "不能降级最后一个启用的管理员");
        }
        userMapper.updateRole(id, request.role(), LocalDateTime.now(clock));
        if (request.nickname() != null) {
            userMapper.updateProfile(id, request.nickname(), target.getPhone(), LocalDateTime.now(clock));
        }
        return AuthService.toView(userMapper.findById(id));
    }

    @Transactional
    public UserView setEnabled(LoginUser operator, Long id, SetEnabledRequest request) {
        User target = requireStaff(id);
        if (target.getId().equals(operator.id())) {
            throw new BizException(ErrorCode.FORBIDDEN, "不能禁用自己");
        }
        if (target.getRole().equals("ADMIN") && Boolean.TRUE.equals(target.getEnabled())
                && !Boolean.TRUE.equals(request.enabled()) && userMapper.countEnabledAdmins() <= 1) {
            throw new BizException(ErrorCode.CONFLICT, "不能禁用最后一个启用的管理员");
        }
        userMapper.updateEnabled(id, request.enabled(), LocalDateTime.now(clock));
        return AuthService.toView(userMapper.findById(id));
    }

    @Transactional
    public void resetPassword(Long id, ResetPasswordRequest request) {
        requireStaff(id);
        userMapper.updatePasswordHash(id, passwordEncoder.encode(request.password()), LocalDateTime.now(clock));
    }

    private User requireStaff(Long id) {
        User user = userMapper.findById(id);
        if (user == null || !("OPERATOR".equals(user.getRole()) || "ADMIN".equals(user.getRole()))) {
            throw new BizException(ErrorCode.NOT_FOUND, "员工不存在");
        }
        return user;
    }
}
