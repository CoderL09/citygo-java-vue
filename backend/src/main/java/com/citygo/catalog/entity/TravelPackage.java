package com.citygo.catalog.entity;
import lombok.Data;
import java.time.LocalDateTime;
@Data
public class TravelPackage {
    private Long id;
    private Long categoryId;
    private String name;
    private String description;
    private Integer packagePriceCents;
    private Integer version;
    private Long familyId;
    private String status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
