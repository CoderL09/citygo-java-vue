package com.citygo.cart.controller;
import com.citygo.cart.service.CartService;
import com.citygo.auth.security.LoginUser;
import com.citygo.common.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/cart") @RequiredArgsConstructor
public class CartController {
    private final CartService service;
    @GetMapping public ApiResponse<List<CartService.Row>> list(@AuthenticationPrincipal LoginUser u){return ApiResponse.ok(service.list(u.id()));}
    @PostMapping public ApiResponse<Void> add(@AuthenticationPrincipal LoginUser u,@Valid @RequestBody CartService.Add r){service.add(u.id(),r);return ApiResponse.ok();}
    @PutMapping("/{id}") public ApiResponse<Void> update(@AuthenticationPrincipal LoginUser u,@PathVariable Long id,@Valid @RequestBody CartService.Update r){service.update(u.id(),id,r);return ApiResponse.ok();}
    @DeleteMapping("/{id}") public ApiResponse<Void> delete(@AuthenticationPrincipal LoginUser u,@PathVariable Long id){service.delete(u.id(),id);return ApiResponse.ok();}
    @DeleteMapping public ApiResponse<Void> clear(@AuthenticationPrincipal LoginUser u){service.clear(u.id());return ApiResponse.ok();}
}
