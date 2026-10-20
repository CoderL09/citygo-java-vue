package com.citygo.common.redis;
import com.citygo.notification.mapper.NotificationMapper;
import com.citygo.auth.service.AuthChanged;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.*;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.*;
import java.time.*;
import java.util.UUID;
@Aspect @Component @Order(200) @RequiredArgsConstructor public class CatalogCommitAspect {
    private final NotificationMapper outbox;private final PublicCache cache;private final Clock clock;private final ApplicationEventPublisher events;
    @Around("@annotation(org.springframework.transaction.annotation.Transactional) && (execution(* com.citygo.catalog.service.*.*(..)) || execution(* com.citygo.settings.service.*.*(..)))")
    public Object invalidateAfterCommit(ProceedingJoinPoint point)throws Throwable{
        Object value=point.proceed();
        if(!TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("Catalog invalidation must join the business transaction");
        outbox.enqueue("CACHE_INVALIDATE","{}","CACHE_INVALIDATE:"+UUID.randomUUID(),LocalDateTime.now(clock));
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){public void afterCommit(){try{cache.invalidate();}catch(RuntimeException ignored){/* durable outbox retries; public reads can still fall back */}}});
        return value;
    }
    @AfterReturning("execution(* com.citygo.staff.service.StaffService.update(..)) || execution(* com.citygo.staff.service.StaffService.setEnabled(..)) || execution(* com.citygo.staff.service.StaffService.resetPassword(..))")
    public void staffChanged(JoinPoint point){Object[] args=point.getArgs();Long id=(Long)args["resetPassword".equals(point.getSignature().getName())?0:1];
        Runnable publish=()->events.publishEvent(new AuthChanged(id,null));
        if(TransactionSynchronizationManager.isSynchronizationActive())TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){public void afterCommit(){publish.run();}});else publish.run();
    }
}
