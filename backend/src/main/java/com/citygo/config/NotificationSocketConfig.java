package com.citygo.config;
import com.citygo.notification.service.*;
import com.citygo.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.http.server.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.Map;
@Configuration @EnableWebSocket @RequiredArgsConstructor public class NotificationSocketConfig implements WebSocketConfigurer {
    private final NotificationSocket socket;private final WsTicketService tickets;
    @Value("${citygo.websocket.allowed-origins:http://127.0.0.1:5175,http://localhost:5175,http://127.0.0.1:5174,http://localhost:5174,http://127.0.0.1:5173,http://localhost:5173}")private String origins;
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry){registry.addHandler(socket,"/ws").setAllowedOrigins(origins.split(",")).addInterceptors(new HandshakeInterceptor(){
        public boolean beforeHandshake(ServerHttpRequest request,ServerHttpResponse response,org.springframework.web.socket.WebSocketHandler handler,Map<String,Object> attrs){
            try{String ticket=UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams().getFirst("ticket");attrs.put(NotificationSocket.AUTH,tickets.consume(ticket));return true;}
            catch(BizException e){response.setStatusCode(e.getErrorCode().getCode()==50300?HttpStatus.SERVICE_UNAVAILABLE:HttpStatus.FORBIDDEN);return false;}
        }
        public void afterHandshake(ServerHttpRequest r,ServerHttpResponse s,org.springframework.web.socket.WebSocketHandler h,Exception e){}
    });}
}
