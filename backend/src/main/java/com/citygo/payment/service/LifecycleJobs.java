package com.citygo.payment.service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.*;
import org.springframework.beans.factory.annotation.Value;
@Component @EnableScheduling @RequiredArgsConstructor
public class LifecycleJobs {
    private final PaymentService service;
    @Value("${citygo.jobs.enabled:true}")private boolean enabled;
    @Scheduled(fixedDelayString="${citygo.jobs.interval-ms:30000}")
    public void tick(){if(!enabled)return;service.reconcilePayments();service.closeExpired();service.reconcileRefunds();}
}
