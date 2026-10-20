package com.citygo.auth.service;
import com.citygo.common.redis.RedisKeys;
import com.citygo.common.api.ErrorCode;
import com.citygo.common.exception.BizException;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.context.ApplicationEventPublisher;
import java.time.*;
@Service @RequiredArgsConstructor public class TokenRevocationService {
    private final StringRedisTemplate redis;private final RedisKeys keys;private final JwtService jwt;private final Clock clock;private final ApplicationEventPublisher events;
    public boolean revoked(String jti){
        if(jti==null||jti.isBlank())throw new BizException(ErrorCode.UNAUTHORIZED);
        try{return Boolean.TRUE.equals(redis.hasKey(keys.key("auth:revoked:"+jti)));}catch(DataAccessException e){throw new BizException(ErrorCode.SERVICE_UNAVAILABLE,"认证依赖暂不可用，请稍后重试");}
    }
    public void logout(String bearer){
        Claims claims=jwt.parse(bearer.substring(7));Duration ttl=Duration.between(clock.instant(),claims.getExpiration().toInstant());
        if(ttl.isNegative()||ttl.isZero())return;
        try{redis.opsForValue().set(keys.key("auth:revoked:"+claims.getId()),"1",ttl);}catch(DataAccessException e){throw new BizException(ErrorCode.SERVICE_UNAVAILABLE,"退出撤销未完成，请稍后重试");}
        events.publishEvent(new AuthChanged(claims.get("uid",Long.class),claims.getId()));
    }
}
