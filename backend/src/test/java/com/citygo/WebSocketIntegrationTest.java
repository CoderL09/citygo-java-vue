package com.citygo;
import com.citygo.booking.service.BookingService;
import com.citygo.notification.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import java.net.*;
import java.net.http.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketIntegrationTest extends P2Fixtures {
    @LocalServerPort int port;@Autowired BookingService booking;@Autowired OutboxWorker worker;@Autowired NotificationSocket sockets;
    long uid,order;String token;
    @BeforeEach void fixture()throws Exception{uid=insertUser("wsuser","secret123","USER",true);long c=contact(uid),s=session(2,1000);order=booking.create(uid,"ws-order-0001",request(c,line("SESSION",s,1))).order().id();token=loginToken("wsuser","secret123");}
    String ticket()throws Exception{var r=mockMvc.perform(post("/api/ws-ticket").header("Authorization",bearer(token))).andExpect(status().isOk()).andReturn();return objectMapper.readTree(r.getResponse().getContentAsString()).at("/data/ticket").asText();}
    static class Listener implements WebSocket.Listener {final BlockingQueue<String> texts=new LinkedBlockingQueue<>();final CountDownLatch closed=new CountDownLatch(1);StringBuilder text=new StringBuilder();public void onOpen(WebSocket w){w.request(1);}public CompletionStage<?> onText(WebSocket w,CharSequence data,boolean last){text.append(data);if(last){texts.add(text.toString());text.setLength(0);}w.request(1);return null;}public CompletionStage<?> onClose(WebSocket w,int code,String reason){closed.countDown();return null;}}
    WebSocket connect(String ticket,Listener listener)throws Exception{return HttpClient.newHttpClient().newWebSocketBuilder().buildAsync(URI.create("ws://127.0.0.1:"+port+"/ws?ticket="+ticket),listener).get(10,TimeUnit.SECONDS);}
    @Test void oneUseTicketReceivesDurableNoticeAndLogoutDisconnects()throws Exception{
        String ticket=ticket();Listener listener=new Listener();WebSocket socket=connect(ticket,listener);
        try{assertThatThrownBy(()->connect(ticket,new Listener())).isInstanceOf(ExecutionException.class).hasCauseInstanceOf(WebSocketHandshakeException.class);
            worker.drain();String pushed=listener.texts.poll(10,TimeUnit.SECONDS);assertThat(pushed).isNotNull();assertThat(objectMapper.readTree(pushed).get("title").asText()).isEqualTo("新预约订单");
            mockMvc.perform(post("/api/auth/logout").header("Authorization",bearer(token))).andExpect(status().isOk());assertThat(listener.closed.await(10,TimeUnit.SECONDS)).isTrue();
        }finally{socket.abort();}
    }
    @Test void roleChangeDisconnectsAndOfflineNotificationsRemainPullable()throws Exception{
        Listener listener=new Listener();WebSocket socket=connect(ticket(),listener);
        try{jdbcTemplate.update("UPDATE users SET role='OPERATOR' WHERE id=?",uid);sockets.checkConnections();assertThat(listener.closed.await(10,TimeUnit.SECONDS)).isTrue();worker.drain();
            mockMvc.perform(get("/api/notifications").header("Authorization",bearer(token))).andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1));
        }finally{socket.abort();}
    }
    @Test void forgedTicketCannotConnect(){assertThatThrownBy(()->connect("00000000-0000-0000-0000-000000000000",new Listener())).isInstanceOf(ExecutionException.class).hasCauseInstanceOf(WebSocketHandshakeException.class);}
}
