package com.citygo.catalog.controller;

import com.citygo.catalog.dto.SessionCreateRequest;
import com.citygo.catalog.dto.SessionStatusRequest;
import com.citygo.catalog.dto.SessionUpdateRequest;
import com.citygo.catalog.dto.SessionView;
import com.citygo.catalog.service.SessionService;
import com.citygo.common.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 场次管理（OPERATOR/ADMIN）。
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminSessionController {

    private final SessionService sessionService;

    @GetMapping("/activities/{activityId}/sessions")
    public ApiResponse<List<SessionView>> list(@PathVariable Long activityId) {
        return ApiResponse.ok(sessionService.listByActivity(activityId, false));
    }

    @PostMapping("/activities/{activityId}/sessions")
    public ApiResponse<SessionView> create(@PathVariable Long activityId,
                                           @Valid @RequestBody SessionCreateRequest request) {
        return ApiResponse.ok(sessionService.create(activityId, request));
    }

    @PutMapping("/sessions/{id}")
    public ApiResponse<SessionView> update(@PathVariable Long id,
                                           @Valid @RequestBody SessionUpdateRequest request) {
        return ApiResponse.ok(sessionService.update(id, request));
    }

    @PutMapping("/sessions/{id}/status")
    public ApiResponse<SessionView> setStatus(@PathVariable Long id,
                                              @Valid @RequestBody SessionStatusRequest request) {
        return ApiResponse.ok(sessionService.setStatus(id, request));
    }

    @DeleteMapping("/sessions/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        sessionService.delete(id);
        return ApiResponse.ok();
    }
}
