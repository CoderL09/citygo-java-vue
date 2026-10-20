package com.citygo.catalog.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * activity_images 表：活动相册与 file_objects 的引用关系。
 */
@Data
public class ActivityImage {

    private Long id;
    private Long activityId;
    private Long fileObjectId;
    private Integer sortOrder;
    private LocalDateTime createdAt;

    /** 联表查询携带（非本表字段） */
    private String storageKey;
    private String originalName;
}
