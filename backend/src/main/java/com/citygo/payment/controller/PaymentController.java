package com.citygo.payment.controller;
import com.citygo.payment.service.PaymentService;
import com.citygo.auth.security.LoginUser;
import com.citygo.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor public class PaymentController {
    private final PaymentService service;
    public record Pay(@NotBlank @Pattern(regexp="SUCCESS|FAIL|TIMEOUT|DELAYED")String scenario){}
    public record Reason(@NotBlank @Size(max=255)String reason){}
    @PostMapping("/api/orders/{id}/payment") public ApiResponse<PaymentService.PaymentView> pay(@AuthenticationPrincipal LoginUser u,@PathVariable Long id,@Valid @RequestBody Pay r){return ApiResponse.ok(service.pay(u.id(),id,r.scenario()));}
    @GetMapping("/api/orders/{id}/payments") public ApiResponse<PaymentService.PaymentView> records(@AuthenticationPrincipal LoginUser u,@PathVariable Long id){return ApiResponse.ok(service.records(u.id(),id));}
    @PostMapping("/api/orders/{id}/cancel") public ApiResponse<?> cancel(@AuthenticationPrincipal LoginUser u,@PathVariable Long id){return ApiResponse.ok(service.cancel(u.id(),id));}
    @PostMapping("/api/orders/{id}/refund") public ApiResponse<?> refund(@AuthenticationPrincipal LoginUser u,@PathVariable Long id,@Valid @RequestBody Reason r){return ApiResponse.ok(service.refund(u.id(),id,r.reason()));}
}
