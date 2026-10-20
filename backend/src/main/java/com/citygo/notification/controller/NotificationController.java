package com.citygo.notification.controller;
import com.citygo.notification.service.*;
import com.citygo.auth.security.LoginUser;
import com.citygo.common.api.ApiResponse;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @Validated @RequiredArgsConstructor public class NotificationController {
    private final NotificationService service;private final WsTicketService tickets;
    @GetMapping("/api/notifications")public ApiResponse<?> since(@AuthenticationPrincipal LoginUser u,@RequestParam(defaultValue="0")@Min(0)long after,@RequestParam(defaultValue="50")@Min(1)@Max(100)int limit){return ApiResponse.ok(service.since(u.id(),after,limit));}
    @GetMapping("/api/notifications/unread")public ApiResponse<?> unread(@AuthenticationPrincipal LoginUser u){return ApiResponse.ok(Map.of("count",service.unread(u.id())));}
    @PutMapping("/api/notifications/{id}/read")public ApiResponse<Void> read(@AuthenticationPrincipal LoginUser u,@PathVariable Long id){service.read(u.id(),id);return ApiResponse.ok();}
    @PutMapping("/api/notifications/read-all")public ApiResponse<Void> readAll(@AuthenticationPrincipal LoginUser u){service.readAll(u.id());return ApiResponse.ok();}
    @PostMapping("/api/ws-ticket")public ApiResponse<?> ticket(@AuthenticationPrincipal LoginUser u,@RequestHeader("Authorization")String bearer){return ApiResponse.ok(Map.of("ticket",tickets.issue(u,bearer),"ttlSeconds",30));}
    @PostMapping("/api/orders/{id}/urge")public ApiResponse<Void> urge(@AuthenticationPrincipal LoginUser u,@PathVariable Long id){service.urge(u.id(),id);return ApiResponse.ok();}
}
