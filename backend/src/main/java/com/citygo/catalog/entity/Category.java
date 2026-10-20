package com.citygo.catalog.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * categories 表：类别类型 ACTIVITY | PACKAGE。
 */
@Data
public class Category {

    private Long id;
    private String categoryType;
    private String name;
    private Integer sortOrder;
    private Boolean status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
