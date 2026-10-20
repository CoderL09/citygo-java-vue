package com.citygo.payment.controller;
import com.citygo.payment.service.PaymentService;
import com.citygo.payment.mapper.LifecycleMapper;
import com.citygo.auth.security.LoginUser;
import com.citygo.common.api.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
@RestController @Validated @RequiredArgsConstructor public class AdminOrderController {
    private final PaymentService service;private final LifecycleMapper mapper;
    @GetMapping("/api/admin/orders") public ApiResponse<?> page(@RequestParam(required=false)String status,@RequestParam(required=false)String keyword,@RequestParam(defaultValue="1")@Min(1)int page,@RequestParam(defaultValue="10")@Min(1)@Max(100)int pageSize){return ApiResponse.ok(service.adminPage(status,keyword,page,pageSize));}
    @GetMapping("/api/admin/orders/{id}") public ApiResponse<?> detail(@PathVariable Long id){return ApiResponse.ok(service.adminDetail(id));}
    @GetMapping("/api/admin/orders/{id}/payments") public ApiResponse<?> records(@PathVariable Long id){return ApiResponse.ok(service.adminRecords(id));}
    @PostMapping("/api/admin/orders/{id}/confirm") public ApiResponse<?> confirm(@AuthenticationPrincipal LoginUser u,@PathVariable Long id){return ApiResponse.ok(service.confirm(u.id(),id));}
    @PostMapping("/api/admin/orders/{id}/reject") public ApiResponse<?> reject(@AuthenticationPrincipal LoginUser u,@PathVariable Long id,@Valid @RequestBody PaymentController.Reason r){return ApiResponse.ok(service.reject(u.id(),id,r.reason()));}
    @PostMapping("/api/admin/orders/{id}/complete") public ApiResponse<?> complete(@AuthenticationPrincipal LoginUser u,@PathVariable Long id){return ApiResponse.ok(service.complete(u.id(),id));}
    @GetMapping("/api/admin/refunds") public ApiResponse<?> refunds(@RequestParam(defaultValue="1")@Min(1)int page,@RequestParam(defaultValue="10")@Min(1)@Max(100)int pageSize){return ApiResponse.ok(PageResult.of(mapper.adminRefunds((page-1)*pageSize,pageSize),mapper.refundCount(),page,pageSize));}
    @PostMapping("/api/admin/refunds/{id}/retry") public ApiResponse<?> retry(@PathVariable Long id){return ApiResponse.ok(service.retryRefund(id));}
    @GetMapping("/api/admin/payment-anomalies") public ApiResponse<?> anomalies(){return ApiResponse.ok(mapper.anomalies());}
}
