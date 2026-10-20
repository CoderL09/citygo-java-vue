package com.citygo.payment.entity;
import lombok.Data;
import java.time.LocalDateTime;
@Data public class PaymentAttempt {
    private Long id;
    private String requestNo;
    private Long orderId;
    private String channel;
    private Integer amountCents;
    private String status;
    private String externalTradeNo;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
