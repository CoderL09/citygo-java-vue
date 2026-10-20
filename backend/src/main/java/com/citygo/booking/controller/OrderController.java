package com.citygo.booking.controller;
import com.citygo.booking.service.BookingService;
import com.citygo.booking.dto.CheckoutRequest;
import com.citygo.auth.security.LoginUser;
import com.citygo.common.api.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/orders") @Validated @RequiredArgsConstructor
public class OrderController {
    private final BookingService service;
    @PostMapping("/preview") public ApiResponse<BookingService.Preview> preview(@AuthenticationPrincipal LoginUser u,@Valid @RequestBody CheckoutRequest r){return ApiResponse.ok(service.preview(u.id(),r));}
    @PostMapping public ApiResponse<BookingService.Detail> create(@AuthenticationPrincipal LoginUser u,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody CheckoutRequest r){return ApiResponse.ok(service.create(u.id(),key,r));}
    @GetMapping public ApiResponse<PageResult<BookingService.Summary>> page(@AuthenticationPrincipal LoginUser u,@RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="10") @Min(1) @Max(100) int pageSize){return ApiResponse.ok(service.page(u.id(),page,pageSize));}
    @GetMapping("/{id}") public ApiResponse<BookingService.Detail> detail(@AuthenticationPrincipal LoginUser u,@PathVariable Long id){return ApiResponse.ok(service.detail(u.id(),id));}
}
