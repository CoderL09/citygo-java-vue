package com.citygo.catalog.controller;

import com.citygo.catalog.dto.ActivityDetailView;
import com.citygo.catalog.dto.ActivityView;
import com.citygo.catalog.dto.CategoryView;
import com.citygo.catalog.dto.SessionView;
import com.citygo.catalog.service.ActivityService;
import com.citygo.catalog.service.CategoryService;
import com.citygo.catalog.service.SessionService;
import com.citygo.common.api.ApiResponse;
import com.citygo.common.api.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;

/**
 * 公开浏览接口（SecurityConfig permitAll）：分类/活动/场次。
 */
@Validated
@RestController
@RequiredArgsConstructor
public class PublicCatalogController {

    private final CategoryService categoryService;
    private final com.citygo.common.redis.PublicCache cache;
    private final ActivityService activityService;
    private final SessionService sessionService;

    @GetMapping("/api/categories")
    public ApiResponse<PageResult<CategoryView>> categories(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
        return ApiResponse.ok(cache.page("categories:"+type+":"+page+":"+pageSize,CategoryView.class,()->categoryService.pagePublic(type, page, pageSize)));
    }

    @GetMapping("/api/activities")
    public ApiResponse<PageResult<ActivityView>> activities(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
        return ApiResponse.ok(cache.page("activities:"+categoryId+":"+keyword+":"+page+":"+pageSize,ActivityView.class,()->activityService.pagePublic(categoryId, keyword, page, pageSize)));
    }

    @GetMapping("/api/activities/{id}")
    public ApiResponse<ActivityDetailView> activityDetail(@PathVariable Long id) {
        return ApiResponse.ok(cache.get("activity:"+id,ActivityDetailView.class,()->activityService.detailPublic(id)));
    }

    @GetMapping("/api/activities/{id}/sessions")
    public ApiResponse<List<SessionView>> sessions(@PathVariable Long id) {
        return ApiResponse.ok(sessionService.listByActivity(id, true));
    }
}
