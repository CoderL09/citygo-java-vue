package com.citygo.catalog.dto;

/**
 * 活动图片视图：url 为后端 /api/files/{storageKey}（local 与 s3 适配统一由后端代理）。
 */
public record ActivityImageView(Long id, Long activityId, Long fileObjectId, String url, Integer sortOrder) {
}
