package com.citygo.booking.entity;
import lombok.Data;
import java.time.LocalDateTime;
@Data
public class OrderItem {
    private Long id;
    private Long orderId;
    private String itemType;
    private Long itemId;
    private String name;
    private Long sessionId;
    @com.fasterxml.jackson.annotation.JsonRawValue
    private String sessionSnapshot;
    @com.fasterxml.jackson.annotation.JsonRawValue
    private String packageSnapshot;
    private Integer quantity;
    private Integer unitPriceCents;
    private Integer lineTotalCents;
    private LocalDateTime createdAt;
}
