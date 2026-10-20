package com.citygo.cart.entity;
import lombok.Data;
import java.time.LocalDateTime;
@Data
public class CartItem {
    private Long id;
    private Long userId;
    private String itemType;
    private Long itemId;
    private Integer quantity;
    private Boolean selected;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
