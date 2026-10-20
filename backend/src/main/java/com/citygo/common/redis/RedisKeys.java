package com.citygo.common.redis;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
@Component public class RedisKeys {
    private final String prefix;
    public RedisKeys(@Value("${citygo.redis.namespace:citygo:dev}")String prefix){this.prefix=prefix+":";}
    public String key(String suffix){return prefix+suffix;}
}
