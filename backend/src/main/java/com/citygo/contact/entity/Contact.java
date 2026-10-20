package com.citygo.contact.entity;
import lombok.Data;
import java.time.LocalDateTime;
@Data
public class Contact {
    private Long id;
    private Long userId;
    private String name;
    private String phone;
    private Boolean defaultContact;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
