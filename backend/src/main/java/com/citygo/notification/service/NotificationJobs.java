package com.citygo.notification.service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
@Component @RequiredArgsConstructor public class NotificationJobs {
    private final OutboxWorker worker;
    @Value("${citygo.jobs.enabled:true}")private boolean enabled;
    @Scheduled(fixedDelay=2000)public void tick(){if(enabled)worker.drain();}
    @Scheduled(fixedDelay=60000)public void remind(){if(enabled)worker.reminders();}
}
