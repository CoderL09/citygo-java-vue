package com.citygo.notification.entity;
import lombok.Data;
import java.time.LocalDateTime;
@Data public class Notice {private Long id;private String eventType;private String title;private String content;private String bizKey;private LocalDateTime createdAt;private Boolean read;}
