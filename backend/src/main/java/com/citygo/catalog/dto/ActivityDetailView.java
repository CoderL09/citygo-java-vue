package com.citygo.catalog.dto;

import java.util.List;

/**
 * 公开详情：活动 + 相册 + 在售场次。
 */
public record ActivityDetailView(Long id, Long categoryId, String categoryName, String name,
                                 String description, String address, String status,
                                 List<ActivityImageView> images, List<SessionView> sessions) {
}
