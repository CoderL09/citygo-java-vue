package com.citygo.file.storage;

import com.citygo.common.api.ErrorCode;
import com.citygo.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 本地磁盘适配器。存储键由服务端生成（yyyy/MM/uuid.ext），
 * 读取前校验键格式与解析路径必须落在 baseDir 内，防路径穿越（实施书 §12）。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "citygo.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private static final String SAFE_KEY = "^[0-9a-zA-Z/._-]+$";

    private final Path baseDir;

    public LocalStorageService(@Value("${citygo.storage.local.base-dir:./data/uploads}") String baseDir) {
        this.baseDir = Paths.get(baseDir).toAbsolutePath().normalize();
    }

    @Override
    public void store(MultipartFile file, String storageKey) {
        Path target = resolve(storageKey);
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException e) {
            log.error("local store failed: {}", storageKey, e);
            throw new BizException(ErrorCode.INTERNAL_ERROR, "文件保存失败");
        }
    }

    @Override
    public Resource load(String storageKey) {
        Path target = resolve(storageKey);
        if (!Files.isRegularFile(target)) {
            throw new BizException(ErrorCode.NOT_FOUND, "文件不存在");
        }
        return new FileSystemResource(target);
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (IOException e) {
            log.warn("local delete failed: {}", storageKey);
            throw new BizException(ErrorCode.SERVICE_UNAVAILABLE,"文件删除暂不可用");
        }
    }

    private Path resolve(String storageKey) {
        if (storageKey == null || !storageKey.matches(SAFE_KEY) || storageKey.contains("..")) {
            throw new BizException(ErrorCode.BAD_REQUEST, "非法文件路径");
        }
        Path resolved = baseDir.resolve(storageKey).normalize();
        if (!resolved.startsWith(baseDir)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "非法文件路径");
        }
        return resolved;
    }
}
