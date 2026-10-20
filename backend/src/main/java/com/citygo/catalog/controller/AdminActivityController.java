package com.citygo.catalog.controller;

import com.citygo.catalog.dto.ActivityCreateRequest;
import com.citygo.catalog.dto.ActivityStatusRequest;
import com.citygo.catalog.dto.ActivityUpdateRequest;
import com.citygo.catalog.dto.ActivityView;
import com.citygo.catalog.dto.ActivityImageView;
import com.citygo.catalog.dto.ImageRefRequest;
import com.citygo.catalog.dto.SortOrderRequest;
import com.citygo.catalog.service.ActivityService;
import com.citygo.auth.security.LoginUser;
import com.citygo.common.api.ApiResponse;
import com.citygo.common.api.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
 * 活动管理（OPERATOR/ADMIN）。
 */
@Validated
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminActivityController {

    private final ActivityService activityService;

    @GetMapping("/activities")
    public ApiResponse<PageResult<ActivityView>> page(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize) {
        return ApiResponse.ok(activityService.pageAdmin(categoryId, status, keyword, page, pageSize));
    }

    @GetMapping("/activities/{id}")
    public ApiResponse<com.citygo.catalog.dto.ActivityDetailView> detail(@PathVariable Long id) {
        return ApiResponse.ok(activityService.detailAdmin(id));
    }

    @PostMapping("/activities")
    public ApiResponse<ActivityView> create(@AuthenticationPrincipal LoginUser operator,
                                            @Valid @RequestBody ActivityCreateRequest request) {
        return ApiResponse.ok(activityService.create(operator.id(), request));
    }

    @PutMapping("/activities/{id}")
    public ApiResponse<ActivityView> update(@PathVariable Long id,
                                            @Valid @RequestBody ActivityUpdateRequest request) {
        return ApiResponse.ok(activityService.update(id, request));
    }

    @PutMapping("/activities/{id}/status")
    public ApiResponse<ActivityView> setStatus(@PathVariable Long id,
                                               @Valid @RequestBody ActivityStatusRequest request) {
        return ApiResponse.ok(activityService.setStatus(id, request));
    }

    @DeleteMapping("/activities/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        activityService.delete(id);
        return ApiResponse.ok();
    }

    @PostMapping("/activities/{id}/images")
    public ApiResponse<ActivityImageView> addImage(@AuthenticationPrincipal LoginUser operator, @PathVariable Long id,
                                                   @Valid @RequestBody ImageRefRequest request) {
        return ApiResponse.ok(activityService.addImage(operator, id, request));
    }

    @PutMapping("/activity-images/{imageId}/sort")
    public ApiResponse<Void> updateImageSort(@PathVariable Long imageId,
                                             @Valid @RequestBody SortOrderRequest request) {
        activityService.updateImageSort(imageId, request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/activity-images/{imageId}")
    public ApiResponse<Void> removeImage(@PathVariable Long imageId) {
        activityService.removeImage(imageId);
        return ApiResponse.ok();
    }
}
