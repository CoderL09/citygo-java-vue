package com.citygo.file.controller;

import com.citygo.auth.security.LoginUser;
import com.citygo.common.api.ApiResponse;
import com.citygo.file.service.FileService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传（OPERATOR/ADMIN）与公开读取（/api/files/** permitAll）。
 * 图片读取统一走后端存储适配器，local/s3 行为一致。
 */
@RestController
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping("/api/admin/files")
    public ApiResponse<FileService.FileView> upload(@AuthenticationPrincipal LoginUser operator,
                                                    @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(fileService.upload(operator, file));
    }

    @DeleteMapping("/api/admin/files/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal LoginUser operator, @PathVariable Long id) {
        fileService.delete(operator, id);
        return ApiResponse.ok();
    }

    @GetMapping("/api/files/**")
    public ResponseEntity<Resource> load(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String storageKey = uri.substring(uri.indexOf("/api/files/") + "/api/files/".length());
        Resource resource = fileService.load(storageKey);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mediaTypeOf(storageKey)))
                .body(resource);
    }

    private String mediaTypeOf(String key) {
        String lower = key.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }
}
