package com.citygo.notification.service;
import com.citygo.notification.entity.*;
import com.citygo.notification.mapper.NotificationMapper;
import com.citygo.payment.mapper.LifecycleMapper;
import com.citygo.common.redis.PublicCache;
import com.fasterxml.jackson.databind.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import java.util.*;
@Service @RequiredArgsConstructor public class OutboxWorker {
    private final NotificationMapper mapper;private final LifecycleMapper orders;private final NotificationSocket sockets;private final PublicCache cache;private final ObjectMapper json;private final PlatformTransactionManager manager;private final Clock clock;
    private record Dispatch(boolean cache,Notice notice,Set<Long> recipients){}
    public void drain(){for(Long id:mapper.due(LocalDateTime.now(clock)))try{
        OutboxEvent pre=mapper.event(id);if(pre==null||"DONE".equals(pre.getStatus()))continue;
        if(!"CACHE_INVALIDATE".equals(pre.getEventType()))cache.invalidate();
        TransactionTemplate tx=new TransactionTemplate(manager);tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        Dispatch dispatch=tx.execute(status->{
            OutboxEvent e=mapper.lockEvent(id);if(e==null||"DONE".equals(e.getStatus()))return null;
            if("CACHE_INVALIDATE".equals(e.getEventType()))return new Dispatch(true,null,Set.of());
            try{
                JsonNode payload=json.readTree(e.getPayload());Long orderId=payload.get("orderId").asLong();var order="ACTIVITY_REMINDER".equals(e.getEventType())?orders.lockOrder(orderId):orders.order(orderId);
                if(order==null)throw new IllegalStateException("Missing notification order");
                if("ACTIVITY_REMINDER".equals(e.getEventType())&&(!"CONFIRMED".equals(order.getStatus())||(payload.has("version")&&payload.get("version").asInt()!=order.getFulfillmentVersion()))){mapper.done(id,LocalDateTime.now(clock));return null;}
                Notice n=new Notice();n.setEventType(e.getEventType());n.setBizKey(e.getBizKey());n.setCreatedAt(LocalDateTime.now(clock));
                n.setTitle(switch(e.getEventType()){case "ORDER_CREATED"->"新预约订单";case "ORDER_STATUS"->"预约状态更新";case "ORDER_RESCHEDULED"->"预约已改期";case "ORDER_URGE"->"用户催确认";case "ACTIVITY_REMINDER"->"活动即将开始";default->throw new IllegalStateException("Unknown outbox event");});
                n.setContent("订单 "+order.getOrderNo()+" · "+statusText(payload.has("status")?payload.get("status").asText():order.getStatus()));
                mapper.notice(n);n=mapper.byKey(e.getBizKey());n.setRead(false);Set<Long> recipients=new HashSet<>();
                if(!"ORDER_URGE".equals(e.getEventType())){mapper.recipient(n.getId(),"USER",order.getUserId());recipients.add(order.getUserId());}
                if(!"ACTIVITY_REMINDER".equals(e.getEventType()))for(Long staff:mapper.staff())if(recipients.add(staff))mapper.recipient(n.getId(),"STAFF",staff);
                mapper.done(id,LocalDateTime.now(clock));return new Dispatch(false,n,recipients);
            }catch(com.fasterxml.jackson.core.JsonProcessingException e1){throw new IllegalStateException(e1);}
        });
        // Both Redis invalidation and WebSocket sends happen after the notification transaction commits.
        if(dispatch!=null){if(dispatch.cache()){
            cache.invalidate();new TransactionTemplate(manager).executeWithoutResult(status->{OutboxEvent e=mapper.lockEvent(id);if(e!=null&&!"DONE".equals(e.getStatus()))mapper.done(id,LocalDateTime.now(clock));});
        }else sockets.push(dispatch.recipients(),dispatch.notice());}
    }catch(RuntimeException failure){mapper.fail(id,failure.getClass().getSimpleName(),LocalDateTime.now(clock).plusSeconds(30),LocalDateTime.now(clock));}}
    private String statusText(String status){return switch(status){case "PENDING_PAYMENT"->"待支付";case "PENDING_CONFIRMATION"->"待确认";case "CONFIRMED"->"已确认";case "COMPLETED"->"已完成";case "CANCELLED"->"已取消";case "CLOSED"->"已关闭";case "REFUNDING"->"退款中";case "REFUNDED"->"已退款";default->"状态更新";};}
    public void reminders(){var now=LocalDateTime.now(clock);for(Long id:mapper.reminders(now,now.plusHours(1)))try{
        TransactionTemplate tx=new TransactionTemplate(manager);tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        tx.executeWithoutResult(status->{
            // Recheck under the same order lock used by rescheduling; bind payload and key to one revision.
            var order=orders.lockOrder(id);if(order==null||mapper.reminderEligible(id,now,now.plusHours(1))==0)return;
            int version=order.getFulfillmentVersion();try{mapper.enqueue("ACTIVITY_REMINDER",json.writeValueAsString(Map.of("orderId",id,"version",version)),"ACTIVITY_REMINDER:"+id+":"+version,now);}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException(e);}
        });
    }catch(RuntimeException e){orders.jobFailure("activity-reminder",id,e.getClass().getSimpleName(),now);}}
}
