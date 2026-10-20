package com.citygo.notification.service;
import com.citygo.notification.mapper.NotificationMapper;
import com.citygo.notification.entity.*;
import com.citygo.payment.mapper.LifecycleMapper;
import com.citygo.common.redis.RedisKeys;
import com.citygo.common.api.ErrorCode;
import com.citygo.common.exception.BizException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import java.util.*;
@Service @RequiredArgsConstructor public class NotificationService {
    private final NotificationMapper mapper;private final LifecycleMapper orders;private final StringRedisTemplate redis;private final RedisKeys keys;private final ObjectMapper json;private final Clock clock;private final PlatformTransactionManager manager;
    @Value("${citygo.urge.min-interval-seconds:60}")private long cooldown;
    @Value("${citygo.urge.daily-limit:5}")private long dailyLimit;
    public List<Notice> since(Long uid,long after,int limit){return mapper.since(uid,after,limit);}
    public long unread(Long uid){return mapper.unread(uid);}
    public void read(Long uid,Long id){if(mapper.read(uid,id,LocalDateTime.now(clock))==0)throw new BizException(ErrorCode.NOT_FOUND);}
    public void readAll(Long uid){mapper.readAll(uid,LocalDateTime.now(clock));}
    public void urge(Long uid,Long id){
        var initial=orders.order(id);if(initial==null||!uid.equals(initial.getUserId()))throw new BizException(ErrorCode.NOT_FOUND);
        if(!"PENDING_CONFIRMATION".equals(initial.getStatus()))throw new BizException(ErrorCode.CONFLICT,"仅待确认的已付款订单可催确认");
        ZoneId zone=ZoneId.of("Asia/Shanghai");LocalDate day=LocalDate.now(clock.withZone(zone));String nonce=UUID.randomUUID().toString();
        String cool=keys.key("urge:cool:"+id),daily=keys.key("urge:daily:"+id+":"+day);
        long dayTtl=Math.max(1,Duration.between(clock.instant(),day.plusDays(1).atStartOfDay(zone).toInstant()).toSeconds());
        String lua="if redis.call('EXISTS',KEYS[1])==1 then return 0 end; local count=tonumber(redis.call('GET',KEYS[2]) or '0'); if count>=tonumber(ARGV[2]) then return 0 end; redis.call('SET',KEYS[1],ARGV[4],'EX',ARGV[1]); redis.call('INCR',KEYS[2]); redis.call('EXPIRE',KEYS[2],ARGV[3]); return 1";
        Long allowed;try{allowed=redis.execute(new DefaultRedisScript<>(lua,Long.class),List.of(cool,daily),Long.toString(cooldown),Long.toString(dailyLimit),Long.toString(dayTtl),nonce);}catch(RuntimeException e){throw new BizException(ErrorCode.SERVICE_UNAVAILABLE,"催确认暂不可用");}
        if(!Long.valueOf(1).equals(allowed))throw new BizException(ErrorCode.TOO_MANY_REQUESTS,"请勿重复催确认，每单每日最多 5 次");
        try{new TransactionTemplate(manager).executeWithoutResult(status->{var o=orders.lockOrder(id);if(!uid.equals(o.getUserId())||!"PENDING_CONFIRMATION".equals(o.getStatus()))throw new BizException(ErrorCode.CONFLICT,"订单状态已变化");try{mapper.enqueue("ORDER_URGE",json.writeValueAsString(Map.of("orderId",id,"userId",uid)),"ORDER_URGE:"+id+":"+nonce,LocalDateTime.now(clock));}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException(e);}});}
        catch(RuntimeException e){try{redis.execute(new DefaultRedisScript<>("if redis.call('GET',KEYS[1])==ARGV[1] then redis.call('DEL',KEYS[1]); if tonumber(redis.call('GET',KEYS[2]) or '0')>0 then redis.call('DECR',KEYS[2]) end end; return 1",Long.class),List.of(cool,daily),nonce);}catch(RuntimeException ignored){}throw e;}
    }
}
