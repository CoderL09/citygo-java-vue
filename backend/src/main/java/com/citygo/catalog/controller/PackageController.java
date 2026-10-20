package com.citygo.catalog.controller;
import com.citygo.catalog.service.PackageService;
import com.citygo.catalog.dto.*;
import com.citygo.catalog.entity.TravelPackage;
import com.citygo.auth.security.LoginUser;
import com.citygo.common.api.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
@RestController @Validated @RequiredArgsConstructor
public class PackageController {
    private final PackageService service;
    private final com.citygo.common.redis.PublicCache cache;
    @GetMapping("/api/packages") public ApiResponse<PageResult<TravelPackage>> page(@RequestParam(required=false) Long categoryId,
        @RequestParam(required=false) String keyword,@RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int pageSize){return ApiResponse.ok(cache.page("packages:"+categoryId+":"+keyword+":"+page+":"+pageSize,TravelPackage.class,()->service.page(true,categoryId,keyword,page,pageSize)));}
    @GetMapping("/api/packages/{id}") public ApiResponse<PackageService.PackageDetail> detail(@PathVariable Long id){return ApiResponse.ok(cache.get("package:"+id,PackageService.PackageDetail.class,()->service.detail(id,true)));}
    @GetMapping("/api/admin/packages") public ApiResponse<PageResult<TravelPackage>> adminPage(@RequestParam(required=false) Long categoryId,
        @RequestParam(required=false) String keyword,@RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="10") @Min(1) @Max(100) int pageSize){return ApiResponse.ok(service.page(false,categoryId,keyword,page,pageSize));}
    @GetMapping("/api/admin/packages/{id}") public ApiResponse<PackageService.PackageDetail> adminDetail(@PathVariable Long id){return ApiResponse.ok(service.detail(id,false));}
    @PostMapping("/api/admin/packages") public ApiResponse<PackageService.PackageDetail> create(@AuthenticationPrincipal LoginUser user,@Valid @RequestBody PackageRequest r){return ApiResponse.ok(service.create(user.id(),r,null));}
    @PostMapping("/api/admin/packages/{id}/versions") public ApiResponse<PackageService.PackageDetail> version(@AuthenticationPrincipal LoginUser user,@PathVariable Long id,@Valid @RequestBody PackageRequest r){return ApiResponse.ok(service.create(user.id(),r,id));}
    @PutMapping("/api/admin/packages/{id}/status") public ApiResponse<PackageService.PackageDetail> status(@PathVariable Long id,@Valid @RequestBody ActivityStatusRequest r){return ApiResponse.ok(service.status(id,r.status()));}
}
