package com.citygo.catalog.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * activities 表：状态 ON_SALE | OFF_SALE。
 */
@Data
public class Activity {

    private Long id;
    private Long categoryId;
    private String name;
    private String description;
    private String address;
    private String status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 联表查询携带（非本表字段） */
    private String categoryName;
}
