package com.citygo.notification.service;
import com.citygo.auth.security.LoginUser;
import com.citygo.auth.service.*;
import com.citygo.auth.mapper.UserMapper;
import com.citygo.common.redis.RedisKeys;
import com.citygo.common.api.ErrorCode;
import com.citygo.common.exception.BizException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.time.*;
import java.util.UUID;
@Service @RequiredArgsConstructor public class WsTicketService {
    public record SessionAuth(Long uid,String role,String jti,Instant expiresAt){}
    private final StringRedisTemplate redis;private final RedisKeys keys;private final JwtService jwt;private final TokenRevocationService revocations;private final UserMapper users;private final ObjectMapper json;private final Clock clock;
    public String issue(LoginUser user,String bearer){
        var claims=jwt.parse(bearer.substring(7));SessionAuth auth=new SessionAuth(user.id(),user.role(),claims.getId(),claims.getExpiration().toInstant());String ticket=UUID.randomUUID().toString();
        try{redis.opsForValue().set(keys.key("ws:ticket:"+ticket),json.writeValueAsString(auth),Duration.ofSeconds(30));return ticket;}catch(Exception e){throw new BizException(ErrorCode.SERVICE_UNAVAILABLE);}
    }
    public SessionAuth consume(String ticket){
        if(ticket==null||!ticket.matches("[0-9a-f-]{36}"))throw new BizException(ErrorCode.FORBIDDEN);
        String encoded;try{encoded=redis.opsForValue().getAndDelete(keys.key("ws:ticket:"+ticket));}catch(RuntimeException e){throw new BizException(ErrorCode.SERVICE_UNAVAILABLE);}
        if(encoded==null)throw new BizException(ErrorCode.FORBIDDEN,"连接票据无效或已使用");
        try{SessionAuth auth=json.readValue(encoded,SessionAuth.class);if(!valid(auth))throw new BizException(ErrorCode.FORBIDDEN);return auth;}catch(BizException e){throw e;}catch(Exception e){throw new BizException(ErrorCode.FORBIDDEN);}
    }
    public boolean valid(SessionAuth auth){
        if(auth==null||!clock.instant().isBefore(auth.expiresAt())||revocations.revoked(auth.jti()))return false;
        var user=users.findById(auth.uid());return user!=null&&Boolean.TRUE.equals(user.getEnabled())&&auth.role().equals(user.getRole());
    }
}
