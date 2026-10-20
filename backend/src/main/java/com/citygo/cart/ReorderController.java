package com.citygo.cart;
import com.citygo.cart.service.ReorderService;
import com.citygo.auth.security.LoginUser;
import com.citygo.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor public class ReorderController {private final ReorderService service;@PostMapping("/api/orders/{id}/reorder")public ApiResponse<?> reorder(@AuthenticationPrincipal LoginUser u,@PathVariable Long id){return ApiResponse.ok(service.reorder(u.id(),id));}}
