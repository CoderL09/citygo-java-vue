package com.citygo.payment.gateway;
import java.time.LocalDateTime;
public interface PaymentGateway {
    record Request(String requestNo,String orderNo,int amountCents) {}
    record Result(String requestNo,String orderNo,int amountCents,String status,String externalNo,LocalDateTime occurredAt) {}
    Result pay(Request request,String scenario);
    Result query(Request request);
    Result close(Request request);
    Result refund(Request refund, String originalPaymentNo);
    Result queryRefund(Request refund);
}
