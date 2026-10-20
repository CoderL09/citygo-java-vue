package com.citygo.notification.entity;
import lombok.Data;
import java.time.LocalDateTime;
@Data public class OutboxEvent {private Long id;private String eventType;private String payload;private String bizKey;private String status;private Integer retryCount;private LocalDateTime createdAt;}
