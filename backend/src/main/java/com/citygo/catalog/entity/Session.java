package com.citygo.catalog.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * sessions 表：起止/预约截止、价格（整数分）、容量与剩余名额、状态 ON_SALE | OFF_SALE。
 */
@Data
public class Session {

    private Long id;
    private Long activityId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime bookingDeadline;
    private Integer priceCents;
    private Integer capacity;
    private Integer available;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
