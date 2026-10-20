package com.citygo.payment.controller;
import com.citygo.payment.service.MockCallbackService;
import com.citygo.payment.gateway.PaymentGateway.Result;
import com.citygo.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;
@RestController @Profile({"dev","test"}) @ConditionalOnProperty(name="citygo.payment.mock-enabled",havingValue="true") @RequiredArgsConstructor
public class MockCallbackController {
    private final MockCallbackService service;
    @PostMapping("/api/payment-callbacks/mock") public ApiResponse<Void> receive(@RequestBody Result r,@RequestHeader("X-Mock-Signature")String signature){service.receive(r,signature);return ApiResponse.ok();}
    @PostMapping("/api/payment-callbacks/mock-refund") public ApiResponse<Void> refund(@RequestBody Result r,@RequestHeader("X-Mock-Signature")String signature){service.receiveRefund(r,signature);return ApiResponse.ok();}
}
