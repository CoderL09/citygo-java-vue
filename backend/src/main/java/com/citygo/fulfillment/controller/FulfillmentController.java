package com.citygo.fulfillment.controller;
import com.citygo.fulfillment.service.*;
import com.citygo.auth.security.LoginUser;
import com.citygo.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor public class FulfillmentController {
 private final ScheduleService schedules;private final CheckinService checkins;
 public record Scan(@NotBlank @Size(max=100)String code){}
 @GetMapping("/api/orders/{id}/fulfillment")public ApiResponse<?> own(@AuthenticationPrincipal LoginUser u,@PathVariable Long id){return ApiResponse.ok(schedules.view(u.id(),id));}
 @PostMapping("/api/orders/{id}/reschedule")public ApiResponse<?> reschedule(@AuthenticationPrincipal LoginUser u,@PathVariable Long id,@RequestHeader("Idempotency-Key")String key,@Valid @RequestBody ScheduleService.Request r){return ApiResponse.ok(schedules.reschedule(u.id(),id,key,r));}
 @PostMapping("/api/orders/{id}/checkin-code")public ApiResponse<?> issue(@AuthenticationPrincipal LoginUser u,@PathVariable Long id){return ApiResponse.ok(checkins.issue(u.id(),id));}
 @GetMapping("/api/admin/orders/{id}/fulfillment")public ApiResponse<?> admin(@PathVariable Long id){return ApiResponse.ok(schedules.view(null,id));}
 @PostMapping("/api/admin/checkins/preview")public ApiResponse<?> preview(@Valid @RequestBody Scan r){return ApiResponse.ok(checkins.preview(r.code()));}
 @PostMapping("/api/admin/checkins/redeem")public ApiResponse<?> redeem(@AuthenticationPrincipal LoginUser u,@Valid @RequestBody Scan r){return ApiResponse.ok(checkins.redeem(u.id(),r.code()));}
}
