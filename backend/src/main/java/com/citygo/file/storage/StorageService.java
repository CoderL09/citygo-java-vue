package com.citygo.file.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * 存储抽象：本地与 S3 兼容适配器（实施书 §3/§12）。
 */
public interface StorageService {

    void store(MultipartFile file, String storageKey);

    Resource load(String storageKey);

    void delete(String storageKey);
}
