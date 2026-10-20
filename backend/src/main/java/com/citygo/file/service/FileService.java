package com.citygo.file.service;

import com.citygo.auth.security.LoginUser;
import com.citygo.common.api.ErrorCode;
import com.citygo.common.exception.BizException;
import com.citygo.file.entity.FileObject;
import com.citygo.file.mapper.FileObjectMapper;
import com.citygo.file.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 文件上传/读取/删除。存储键由服务端生成，客户端不可指定路径（实施书 §12）。
 */
@Service
@RequiredArgsConstructor
public class FileService {

    private static final DateTimeFormatter KEY_MONTH = DateTimeFormatter.ofPattern("yyyy/MM");

    private final StorageService storageService;
    private final FileObjectMapper fileObjectMapper;
    private final Clock clock;

    public record FileView(Long id, String url, String originalName, String contentType, Long sizeBytes) {
    }

    @Transactional
    public FileView upload(LoginUser operator, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "请选择文件");
        }
        if (file.getSize() > ImageTypeValidator.MAX_SIZE_BYTES) {
            throw new BizException(ErrorCode.BAD_REQUEST, "文件大小不能超过 5MB");
        }
        String ext;
        try {
            ext = ImageTypeValidator.requireAllowedExt(file.getOriginalFilename());
            ImageTypeValidator.requireMagicMatches(ext, file);
        } catch (IllegalArgumentException e) {
            throw new BizException(ErrorCode.BAD_REQUEST, e.getMessage());
        }
        // 存储键服务端生成，忽略客户端文件名
        String storageKey = LocalDate.now(clock).format(KEY_MONTH) + "/" + UUID.randomUUID() + "." + ext;
        storageService.store(file, storageKey);

        FileObject fo = new FileObject();
        fo.setOwnerId(operator.id());
        fo.setOwnerType("STAFF");
        fo.setStorageKey(storageKey);
        String originalName = file.getOriginalFilename();
        if (originalName != null && originalName.length() > 255) {
            originalName = originalName.substring(originalName.length() - 255);
        }
        fo.setOriginalName(originalName);
        fo.setContentType(switch (ext) {
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> "image/jpeg";
        });
        fo.setSizeBytes(file.getSize());
        fo.setStatus("TEMP");
        fo.setCreatedAt(LocalDateTime.now(clock));
        fileObjectMapper.insert(fo);
        return new FileView(fo.getId(), "/api/files/" + storageKey,
                fo.getOriginalName(), fo.getContentType(), fo.getSizeBytes());
    }

    public Resource load(String storageKey) {
        return storageService.load(storageKey);
    }

    /** 删除：被活动相册引用（USED）时拒绝；物理文件删除失败不阻塞元数据删除（由 P5 清理兜底） */
    @Transactional
    public void delete(LoginUser operator, Long fileId) {
        FileObject fo = fileObjectMapper.lockById(fileId);
        if (fo == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "文件不存在");
        }
        if (!"ADMIN".equals(operator.role()) && !operator.id().equals(fo.getOwnerId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "不能删除其他员工的文件");
        }
        if (fileObjectMapper.countImageRefs(fileId) > 0) {
            throw new BizException(ErrorCode.CONFLICT, "文件已被活动引用，请先移除图片");
        }
        // Commit a tombstone first; retries cannot reattach a file while physical deletion is pending.
        fileObjectMapper.requestDelete(fileId,LocalDateTime.now(clock));
    }
}
