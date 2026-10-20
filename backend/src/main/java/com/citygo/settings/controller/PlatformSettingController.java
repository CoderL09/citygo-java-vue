package com.citygo.settings.controller;

import com.citygo.auth.security.LoginUser;
import com.citygo.common.api.ApiResponse;
import com.citygo.settings.service.PlatformSettingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PlatformSettingController {

    public record AcceptingOrdersView(boolean acceptingOrders) {
    }

    public record AcceptingOrdersRequest(@NotNull Boolean acceptingOrders) {
    }

    private final PlatformSettingService platformSettingService;
    private final com.citygo.common.redis.PublicCache cache;

    /** 公开查询（用户端展示营业状态） */
    @GetMapping("/api/settings/accepting-orders")
    public ApiResponse<AcceptingOrdersView> publicGet() {
        return ApiResponse.ok(cache.get("accepting-orders",AcceptingOrdersView.class,()->new AcceptingOrdersView(platformSettingService.acceptingOrders())));
    }

    /** 管理端读取（OPERATOR/ADMIN） */
    @GetMapping("/api/admin/settings/accepting-orders")
    public ApiResponse<AcceptingOrdersView> adminGet() {
        return ApiResponse.ok(new AcceptingOrdersView(platformSettingService.acceptingOrders()));
    }

    @PutMapping("/api/admin/settings/accepting-orders")
    public ApiResponse<AcceptingOrdersView> update(@AuthenticationPrincipal LoginUser operator,
                                                   @Valid @RequestBody AcceptingOrdersRequest request) {
        return ApiResponse.ok(new AcceptingOrdersView(
                platformSettingService.setAcceptingOrders(request.acceptingOrders(), operator.id())));
    }
}
