package com.citygo.staff.controller;

import com.citygo.auth.dto.UserView;
import com.citygo.auth.security.LoginUser;
import com.citygo.common.api.ApiResponse;
import com.citygo.common.api.PageResult;
import com.citygo.staff.dto.ResetPasswordRequest;
import com.citygo.staff.dto.SetEnabledRequest;
import com.citygo.staff.dto.StaffCreateRequest;
import com.citygo.staff.dto.StaffUpdateRequest;
import com.citygo.staff.service.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 员工管理（仅 ADMIN，见 SecurityConfig /api/admin/staff/**）。
 */
@Validated
@RestController
@RequestMapping("/api/admin/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    @GetMapping
    public ApiResponse<PageResult<UserView>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize) {
        return ApiResponse.ok(staffService.page(keyword, page, pageSize));
    }

    @PostMapping
    public ApiResponse<UserView> create(@Valid @RequestBody StaffCreateRequest request) {
        return ApiResponse.ok(staffService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserView> update(@AuthenticationPrincipal LoginUser operator, @PathVariable Long id,
                                        @Valid @RequestBody StaffUpdateRequest request) {
        return ApiResponse.ok(staffService.update(operator, id, request));
    }

    @PutMapping("/{id}/enabled")
    public ApiResponse<UserView> setEnabled(@AuthenticationPrincipal LoginUser operator, @PathVariable Long id,
                                            @Valid @RequestBody SetEnabledRequest request) {
        return ApiResponse.ok(staffService.setEnabled(operator, id, request));
    }

    @PutMapping("/{id}/password")
    public ApiResponse<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
        staffService.resetPassword(id, request);
        return ApiResponse.ok();
    }
}
