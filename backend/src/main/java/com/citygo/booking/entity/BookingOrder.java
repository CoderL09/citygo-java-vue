package com.citygo.booking.entity;
import lombok.Data;
import java.time.LocalDateTime;
@Data
public class BookingOrder {
    private Long id;
    private Integer fulfillmentVersion;
    private String orderNo;
    private Long userId;
    private Integer totalCents;
    private String status;
    private String idempotencyKey;
    private String requestDigest;
    private String contactName;
    private String contactPhone;
    private String remark;
    private LocalDateTime expireAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
