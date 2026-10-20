package com.citygo.catalog.controller;

import com.citygo.catalog.dto.CategoryCreateRequest;
import com.citygo.catalog.dto.CategoryStatusRequest;
import com.citygo.catalog.dto.CategoryUpdateRequest;
import com.citygo.catalog.dto.CategoryView;
import com.citygo.catalog.service.CategoryService;
import com.citygo.common.api.ApiResponse;
import com.citygo.common.api.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 分类管理（OPERATOR/ADMIN，见 SecurityConfig /api/admin/**）。
 */
@Validated
@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ApiResponse<PageResult<CategoryView>> page(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize) {
        return ApiResponse.ok(categoryService.pageAdmin(type, status, keyword, page, pageSize));
    }

    @PostMapping
    public ApiResponse<CategoryView> create(@Valid @RequestBody CategoryCreateRequest request) {
        return ApiResponse.ok(categoryService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<CategoryView> update(@PathVariable Long id, @Valid @RequestBody CategoryUpdateRequest request) {
        return ApiResponse.ok(categoryService.update(id, request));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<CategoryView> setStatus(@PathVariable Long id, @Valid @RequestBody CategoryStatusRequest request) {
        return ApiResponse.ok(categoryService.setStatus(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ApiResponse.ok();
    }
}
