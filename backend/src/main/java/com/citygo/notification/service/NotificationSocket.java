package com.citygo.notification.service;
import com.citygo.auth.service.AuthChanged;
import com.citygo.notification.entity.Notice;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
@Component @RequiredArgsConstructor public class NotificationSocket extends TextWebSocketHandler {
    public static final String AUTH="citygo.wsAuth";
    private final WsTicketService tickets;private final ObjectMapper json;
    private final Map<String,WebSocketSession> sessions=new ConcurrentHashMap<>();
    @Override public void afterConnectionEstablished(WebSocketSession session)throws Exception{var auth=auth(session);if(!tickets.valid(auth)){session.close(CloseStatus.POLICY_VIOLATION);return;}sessions.put(session.getId(),session);}
    @Override public void afterConnectionClosed(WebSocketSession s,CloseStatus status){sessions.remove(s.getId());}
    @Override protected void handleTextMessage(WebSocketSession s,TextMessage message)throws Exception{if("ping".equals(message.getPayload())){s.sendMessage(new TextMessage("{\"type\":\"pong\"}"));}else s.close(CloseStatus.POLICY_VIOLATION);}
    private WsTicketService.SessionAuth auth(WebSocketSession s){return (WsTicketService.SessionAuth)s.getAttributes().get(AUTH);}
    @Scheduled(fixedDelay=10000)public void checkConnections(){for(var s:sessions.values())try{if(!tickets.valid(auth(s)))close(s);}catch(RuntimeException e){close(s);}}
    @EventListener public void changed(AuthChanged change){for(var s:sessions.values()){var a=auth(s);if(a.uid().equals(change.userId())&&(change.jti()==null||change.jti().equals(a.jti())))close(s);}}
    public void push(Set<Long> recipients,Notice notice){for(var s:sessions.values())if(recipients.contains(auth(s).uid()))try{
        if(!tickets.valid(auth(s))){close(s);continue;}
        synchronized(s){if(s.isOpen())s.sendMessage(new TextMessage(json.writeValueAsString(notice)));}
    }catch(Exception e){close(s);}}
    private void close(WebSocketSession s){sessions.remove(s.getId());try{s.close(CloseStatus.POLICY_VIOLATION);}catch(Exception ignored){}}
}
