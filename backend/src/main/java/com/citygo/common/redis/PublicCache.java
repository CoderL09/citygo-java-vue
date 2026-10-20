package com.citygo.common.redis;
import com.citygo.common.api.PageResult;
import com.fasterxml.jackson.databind.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
@Service @RequiredArgsConstructor public class PublicCache {
    private final StringRedisTemplate redis;private final RedisKeys keys;private final ObjectMapper json;
    @Value("${citygo.cache.ttl-seconds:300}")private long ttl;
    private final AtomicLong hits=new AtomicLong();
    public long hits(){return hits.get();}
    public void afterBusinessCommit(){
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization(){public void afterCommit(){try{invalidate();}catch(RuntimeException ignored){/* order outbox retries invalidation */}}});
    }
    public void invalidate(){redis.opsForValue().increment(keys.key("cache:version"));}
    public <T>T get(String name,Class<T> type,Supplier<T> loader){return load(name,json.constructType(type),loader);}
    public <T>PageResult<T> page(String name,Class<T> type,Supplier<PageResult<T>> loader){return load(name,json.getTypeFactory().constructParametricType(PageResult.class,type),loader);}
    private <T>T load(String name,JavaType type,Supplier<T> loader){
        String key;
        try{
            String version=redis.opsForValue().get(keys.key("cache:version"));
            String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(name.getBytes(StandardCharsets.UTF_8)));
            key=keys.key("cache:"+(version==null?"0":version)+":"+hash);
            String encoded=redis.opsForValue().get(key);if(encoded!=null){T value=json.readValue(encoded,type);hits.incrementAndGet();return value;}
        }catch(Exception e){return loader.get();}
        T value=loader.get();try{redis.opsForValue().set(key,json.writeValueAsString(value),Duration.ofSeconds(ttl));}catch(Exception ignored){}
        return value;
    }
}
