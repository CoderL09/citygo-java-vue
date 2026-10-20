package com.citygo.payment.entity;
import lombok.Data;
import java.time.LocalDateTime;
@Data public class RefundRecord {
    private Long id;
    private String refundNo;
    private Long paymentAttemptId;
    private Long orderId;
    private Integer amountCents;
    private String status;
    private String reason;
    private Long requestedBy;
    private String failReason;
    private Boolean compensation;
    private LocalDateTime successAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
