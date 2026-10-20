package com.citygo;
import com.citygo.booking.service.BookingService;
import com.citygo.payment.service.PaymentService;
import com.citygo.notification.service.*;
import com.citygo.notification.mapper.NotificationMapper;
import com.citygo.common.redis.*;
import com.citygo.common.exception.BizException;
import com.citygo.auth.service.JwtService;
import com.citygo.catalog.service.ActivityService;
import com.citygo.catalog.dto.ActivityStatusRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.data.redis.connection.lettuce.*;
import io.lettuce.core.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@TestPropertySource(properties="citygo.urge.min-interval-seconds=1")
class RedisNotificationIntegrationTest extends P2Fixtures {
    @Autowired BookingService booking;@Autowired PaymentService payments;@Autowired NotificationService notices;@Autowired OutboxWorker worker;@Autowired PublicCache cache;@Autowired RedisKeys keys;@Autowired JwtService jwt;@Autowired ActivityService activities;
    @MockitoSpyBean NotificationMapper mapper;
    long uid,contact,session,order,admin;String token;
    @BeforeEach void fixtures()throws Exception{uid=insertUser("redisuser","secret123","USER",true);admin=insertUser("redisadmin","secret123","ADMIN",true);contact=contact(uid);session=session(2,1000);order=booking.create(uid,"redis-order-001",request(contact,line("SESSION",session,1))).order().id();token=loginToken("redisuser","secret123");}
    @Test void logoutRevokesJtiUntilExpiration()throws Exception{
        var claims=jwt.parse(token);mockMvc.perform(post("/api/auth/logout").header("Authorization",bearer(token))).andExpect(status().isOk());
        mockMvc.perform(get("/api/auth/me").header("Authorization",bearer(token))).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(40103));
        assertThat(redisTemplate.getExpire(keys.key("auth:revoked:"+claims.getId()))).isBetween(1L,1800L);
    }
    @Test void redisConnectionFailureClosesAuthButPublicBrowseFallsBack()throws Exception{
        payments.pay(uid,order,"SUCCESS");var original=redisTemplate.getConnectionFactory();
        var configuration=LettuceClientConfiguration.builder().commandTimeout(Duration.ofMillis(200)).shutdownTimeout(Duration.ZERO)
            .clientOptions(ClientOptions.builder().socketOptions(SocketOptions.builder().connectTimeout(Duration.ofMillis(200)).build()).build()).build();
        var failed=new LettuceConnectionFactory(new org.springframework.data.redis.connection.RedisStandaloneConfiguration("127.0.0.1",1),configuration);failed.afterPropertiesSet();failed.start();
        try{redisTemplate.setConnectionFactory(failed);
            mockMvc.perform(get("/api/auth/me").header("Authorization",bearer(token))).andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value(50300));
            mockMvc.perform(post("/api/auth/logout").header("Authorization",bearer(token))).andExpect(status().isServiceUnavailable());
            mockMvc.perform(get("/api/categories")).andExpect(status().isOk());
            assertThatThrownBy(()->notices.urge(uid,order)).isInstanceOfSatisfying(BizException.class,e->assertThat(e.getErrorCode().getCode()).isEqualTo(50300));
            assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM outbox_events WHERE event_type='ORDER_URGE'",Integer.class)).isZero();
        }finally{redisTemplate.setConnectionFactory(original);failed.destroy();}
    }
    @Test void publicCacheHitsTtlAndPublicationInvalidation()throws Exception{
        long activity=jdbcTemplate.queryForObject("SELECT activity_id FROM sessions WHERE id=?",Long.class,session);
        mockMvc.perform(get("/api/activities/"+activity)).andExpect(status().isOk());long hits=cache.hits();
        mockMvc.perform(get("/api/activities/"+activity)).andExpect(status().isOk());assertThat(cache.hits()).isGreaterThan(hits);
        List<String> entries=new ArrayList<>();try(var cursor=redisTemplate.scan(org.springframework.data.redis.core.ScanOptions.scanOptions().match(keys.key("cache:*:*")).build())){cursor.forEachRemaining(entries::add);}
        assertThat(entries).isNotEmpty();for(String key:entries)assertThat(redisTemplate.getExpire(key)).isBetween(1L,300L);
        activities.setStatus(activity,new ActivityStatusRequest("OFF_SALE"));
        mockMvc.perform(get("/api/activities/"+activity)).andExpect(status().isNotFound());
    }
    @Test void bookingAndReleaseInvalidatePublicAvailability()throws Exception{
        long activity=jdbcTemplate.queryForObject("SELECT activity_id FROM sessions WHERE id=?",Long.class,session);
        mockMvc.perform(get("/api/activities/"+activity)).andExpect(jsonPath("$.data.sessions[0].available").value(1));
        long second=booking.create(uid,"stock-cache-0001",request(contact,line("SESSION",session,1))).order().id();
        mockMvc.perform(get("/api/activities/"+activity)).andExpect(jsonPath("$.data.sessions[0].available").value(0));
        assertThatThrownBy(()->booking.create(uid,"stock-cache-0002",request(contact,line("SESSION",session,1)))).isInstanceOf(BizException.class);
        payments.cancel(uid,second);mockMvc.perform(get("/api/activities/"+activity)).andExpect(jsonPath("$.data.sessions[0].available").value(1));
    }
    @Test void outboxIsDurableAndIdempotentAndNotificationsAreOwned()throws Exception{
        worker.drain();worker.drain();assertThat(count("notifications")).isEqualTo(1);assertThat(count("notification_recipients")).isEqualTo(2);
        long notice=notices.since(uid,0,50).getFirst().getId();assertThat(notices.unread(uid)).isEqualTo(1);
        insertUser("noticealien","secret123","USER",true);String other=loginToken("noticealien","secret123");
        mockMvc.perform(put("/api/notifications/"+notice+"/read").header("Authorization",bearer(other))).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/notifications").header("Authorization",bearer(other))).andExpect(jsonPath("$.data.length()").value(0));
        notices.read(uid,notice);assertThat(notices.unread(uid)).isZero();assertThat(notices.unread(admin)).isEqualTo(1);
    }
    @Test void failedOutboxTransactionRetriesWithoutDuplicateOrPartialNotice(){
        doThrow(new IllegalStateException("injected recipient failure")).when(mapper).recipient(anyLong(),anyString(),anyLong());
        worker.drain();assertThat(count("notifications")).isZero();assertThat(count("notification_recipients")).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM outbox_events WHERE event_type='ORDER_CREATED'",String.class)).isEqualTo("FAILED");
        reset(mapper);jdbcTemplate.update("UPDATE outbox_events SET next_retry_at=UTC_TIMESTAMP(3)-INTERVAL 1 SECOND");worker.drain();worker.drain();
        assertThat(count("notifications")).isEqualTo(1);assertThat(count("notification_recipients")).isEqualTo(2);
    }
    @Test void cooldownAndDailyLimitAreAtomicAndPersistOnlyAcceptedUrges()throws Exception{
        payments.pay(uid,order,"SUCCESS");notices.urge(uid,order);
        assertThatThrownBy(()->notices.urge(uid,order)).isInstanceOfSatisfying(BizException.class,e->assertThat(e.getErrorCode().getCode()).isEqualTo(42900));
        for(int i=0;i<4;i++){Thread.sleep(1100);notices.urge(uid,order);}Thread.sleep(1100);
        assertThatThrownBy(()->notices.urge(uid,order)).isInstanceOfSatisfying(BizException.class,e->assertThat(e.getErrorCode().getCode()).isEqualTo(42900));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM outbox_events WHERE event_type='ORDER_URGE'",Integer.class)).isEqualTo(5);
    }
    @Test void concurrentUrgesOnlyOneAccepted()throws Exception{
        payments.pay(uid,order,"SUCCESS");ExecutorService pool=Executors.newFixedThreadPool(10);List<Integer> results=new ArrayList<>();
        try{List<Callable<Integer>> calls=new ArrayList<>();for(int i=0;i<20;i++)calls.add(()->{try{notices.urge(uid,order);return 1;}catch(BizException e){assertThat(e.getErrorCode().getCode()).isEqualTo(42900);return 0;}});for(var f:pool.invokeAll(calls,20,TimeUnit.SECONDS))results.add(f.get());}
        finally{pool.shutdown();assertThat(pool.awaitTermination(30,TimeUnit.SECONDS)).isTrue();}
        assertThat(results.stream().mapToInt(Integer::intValue).sum()).isEqualTo(1);
    }
}
