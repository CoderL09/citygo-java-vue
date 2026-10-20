package com.citygo.file.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * file_objects 表：上传文件元数据，引用状态 TEMP | USED。
 */
@Data
public class FileObject {

    private Long id;
    private Long ownerId;
    private String ownerType;
    private String storageKey;
    private String originalName;
    private String contentType;
    private Long sizeBytes;
    private String status;
    private LocalDateTime createdAt;
}
