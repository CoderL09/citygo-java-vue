package com.citygo;
import com.citygo.booking.service.BookingService;
import com.citygo.booking.dto.CheckoutRequest;
import com.citygo.common.exception.BizException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BookingIntegrationTest extends P2Fixtures {
    @Autowired BookingService booking;
    @Test void serverPriceAggregatesDuplicateSubsessions_andPersistsSnapshots() throws Exception {
        long uid=insertUser("buyer","secret123","USER",true),contact=contact(uid),a=session(10,1000),b=session(10,2000),p=pkg(2500,a,b);
        String token=loginToken("buyer","secret123");
        String body=objectMapper.writeValueAsString(Map.of("contactId",contact,"items",List.of(line("SESSION",a,1),line("PACKAGE",p,2)),"totalCents",1));
        var result=mockMvc.perform(post("/api/orders").header("Authorization",bearer(token)).header("Idempotency-Key","server-price-001").contentType(APPLICATION_JSON).content(body))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.order.totalCents").value(6000))
            .andExpect(jsonPath("$.data.order.status").value("PENDING_PAYMENT"))
            .andExpect(jsonPath("$.data.items[0].packageSnapshot.packageVersion").value(1)).andReturn();
        long order=objectMapper.readTree(result.getResponse().getContentAsString()).at("/data/order/id").asLong();
        assertThat(stock(a)).isEqualTo(7);assertThat(stock(b)).isEqualTo(8);assertThat(count("order_allocations")).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("SELECT quantity FROM order_allocations WHERE order_id=? AND session_id=?",Integer.class,order,a)).isEqualTo(3);
        assertThat(count("order_events")).isEqualTo(1);assertThat(count("outbox_events")).isEqualTo(1);
        jdbcTemplate.update("UPDATE contacts SET name='新联系人',phone='13900000000' WHERE id=?",contact);
        jdbcTemplate.update("UPDATE activities SET name='新名称',address='新地址'");
        mockMvc.perform(get("/api/orders/"+order).header("Authorization",bearer(token)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.order.contactName").value("张同学"))
            .andExpect(jsonPath("$.data.items[1].sessionSnapshot.activityName").value("城市步行"))
            .andExpect(jsonPath("$.data.items[1].sessionSnapshot.address").value("集合地址"));
    }
    @Test void sameKeySameRequestReturnsOriginal_evenAfterDirectoryAndContactChange() {
        long uid=insertUser("retry","secret123","USER",true),c=contact(uid),s=session(5,1000);
        CheckoutRequest r=request(c,line("SESSION",s,1));var first=booking.create(uid,"same-key-0001",r);
        jdbcTemplate.update("DELETE FROM contacts WHERE id=?",c);jdbcTemplate.update("UPDATE sessions SET status='OFF_SALE' WHERE id=?",s);
        assertThat(booking.create(uid,"same-key-0001",r).order().id()).isEqualTo(first.order().id());
        assertThat(stock(s)).isEqualTo(4);assertThat(count("orders")).isEqualTo(1);
        assertThatThrownBy(()->booking.create(uid,"same-key-0001",request(c,line("SESSION",s,2))))
            .isInstanceOfSatisfying(BizException.class,e->assertThat(e.getErrorCode().getCode()).isEqualTo(40913));
    }
    @Test void packageWithUnavailableChildLeavesAllStockAndOrderTablesUnchanged(){
        long uid=insertUser("rollback","secret123","USER",true),c=contact(uid),a=session(3,1000),b=session(0,1000),p=pkg(1500,a,b);
        assertThatThrownBy(()->booking.create(uid,"package-rollback",request(c,line("PACKAGE",p,1)))).isInstanceOf(BizException.class);
        assertThat(stock(a)).isEqualTo(3);assertThat(stock(b)).isZero();assertNoOrders();
    }
    @Test void insufficientAggregatedDemandRollsBackEarlierConditionalDecrement(){
        long uid=insertUser("aggregate","secret123","USER",true),c=contact(uid),a=session(5,1000),b=session(1,1000),d=session(5,1000);
        long p=pkg(1000,a,b),q=pkg(1000,b,d);
        assertThatThrownBy(()->booking.create(uid,"aggregate-rollback",request(c,line("PACKAGE",p,1),line("PACKAGE",q,1)))).isInstanceOf(BizException.class);
        assertThat(stock(a)).isEqualTo(5);assertThat(stock(b)).isEqualTo(1);assertThat(stock(d)).isEqualTo(5);assertNoOrders();
    }
    @Test void rejectsPausedOffSaleExpiredAndInsufficient_andExistingOrderRemainsReadable() throws Exception {
        long uid=insertUser("sale","secret123","USER",true),c=contact(uid),s=session(3,1000);String token=loginToken("sale","secret123");
        var order=booking.create(uid,"before-pause-001",request(c,line("SESSION",s,1)));
        jdbcTemplate.update("UPDATE platform_settings SET setting_value='false'");
        assertThatThrownBy(()->booking.create(uid,"after-pause-001",request(c,line("SESSION",s,1)))).isInstanceOfSatisfying(BizException.class,e->assertThat(e.getErrorCode().getCode()).isEqualTo(40906));
        mockMvc.perform(get("/api/orders/"+order.order().id()).header("Authorization",bearer(token))).andExpect(status().isOk());
        jdbcTemplate.update("UPDATE platform_settings SET setting_value='true'");jdbcTemplate.update("UPDATE sessions SET status='OFF_SALE' WHERE id=?",s);
        assertThatThrownBy(()->booking.create(uid,"off-sale-0001",request(c,line("SESSION",s,1)))).isInstanceOf(BizException.class);
        jdbcTemplate.update("UPDATE sessions SET status='ON_SALE',booking_deadline=UTC_TIMESTAMP(3)-INTERVAL 1 MINUTE WHERE id=?",s);
        assertThatThrownBy(()->booking.create(uid,"expired-0001",request(c,line("SESSION",s,1)))).isInstanceOf(BizException.class);
        assertThat(stock(s)).isEqualTo(2);assertThat(count("orders")).isEqualTo(1);
    }
    @Test void validatesOwnershipQuantityRowLimitKeyAndOverflow() throws Exception {
        long uid=insertUser("limits","secret123","USER",true),other=insertUser("other","secret123","USER",true),c=contact(uid),alien=contact(other),s=session(10,Integer.MAX_VALUE);
        String token=loginToken("limits","secret123");
        mockMvc.perform(post("/api/orders").header("Authorization",bearer(token)).header("Idempotency-Key","limits-key-01").contentType(APPLICATION_JSON).content(objectMapper.writeValueAsString(request(alien,line("SESSION",s,1))))).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/orders").header("Authorization",bearer(token)).header("Idempotency-Key","limits-key-02").contentType(APPLICATION_JSON).content(objectMapper.writeValueAsString(request(c,line("SESSION",s,6))))).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/orders").header("Authorization",bearer(token)).header("Idempotency-Key","limits-key-03").contentType(APPLICATION_JSON).content(objectMapper.writeValueAsString(new CheckoutRequest(c,Collections.nCopies(11,line("SESSION",s,1)),"")))).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/orders").header("Authorization",bearer(token)).header("Idempotency-Key","x").contentType(APPLICATION_JSON).content(objectMapper.writeValueAsString(request(c,line("SESSION",s,1))))).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/orders").header("Authorization",bearer(token)).header("Idempotency-Key","limits-key-04").contentType(APPLICATION_JSON).content(objectMapper.writeValueAsString(request(c,line("SESSION",s,2))))).andExpect(status().isBadRequest());
        assertNoOrders();assertThat(stock(s)).isEqualTo(10);
    }
    @Test void listAndDetailAreOwned_andEmployeesCannotCreateUserOrders() throws Exception {
        long uid=insertUser("owner","secret123","USER",true),c=contact(uid),s=session(5,1000);var order=booking.create(uid,"owner-order-01",request(c,line("SESSION",s,1)));
        insertUser("intruder","secret123","USER",true);String other=loginToken("intruder","secret123");
        mockMvc.perform(get("/api/orders/"+order.order().id()).header("Authorization",bearer(other))).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/orders").header("Authorization",bearer(other))).andExpect(jsonPath("$.data.total").value(0));
        insertUser("operator","secret123","OPERATOR",true);String op=loginToken("operator","secret123");
        mockMvc.perform(post("/api/orders").header("Authorization",bearer(op)).header("Idempotency-Key","operator-key").contentType(APPLICATION_JSON).content(objectMapper.writeValueAsString(request(c,line("SESSION",s,1))))).andExpect(status().isForbidden());
    }
    @Test void twentyDifferentUsersCompeteForLastSeat_exactlyOneWins() throws Exception {
        long s=session(1,1000);List<Callable<Long>> tasks=new ArrayList<>();
        for(int i=0;i<20;i++){long uid=insertUser("concurrent"+i,"secret123","USER",true),c=contact(uid);String key="last-seat-"+i;
            tasks.add(()->{try{return booking.create(uid,key,request(c,line("SESSION",s,1))).order().id();}catch(BizException e){assertThat(e.getErrorCode().getCode()).isEqualTo(40903);return -1L;}});}
        List<Long> results=parallel(tasks);
        assertThat(results.stream().filter(id->id>0).count()).isEqualTo(1);assertThat(stock(s)).isZero();
        assertThat(count("orders")).isEqualTo(1);assertThat(count("order_items")).isEqualTo(1);assertThat(count("order_allocations")).isEqualTo(1);assertThat(count("order_events")).isEqualTo(1);
    }
    @Test void twentyConcurrentRetriesSameKeyOnlyReserveOnce() throws Exception {
        long uid=insertUser("samekey","secret123","USER",true),c=contact(uid),s=session(20,1000);CheckoutRequest r=request(c,line("SESSION",s,2));
        List<Callable<Long>> tasks=new ArrayList<>();for(int i=0;i<20;i++)tasks.add(()->booking.create(uid,"parallel-same-key",r).order().id());
        assertThat(new HashSet<>(parallel(tasks))).hasSize(1);assertThat(stock(s)).isEqualTo(18);assertThat(count("orders")).isEqualTo(1);assertThat(count("outbox_events")).isEqualTo(1);
    }
    @Test void packageAllocationProtectsChildSessionFromEditing() throws Exception {
        long uid=insertUser("packagebuyer","secret123","USER",true),c=contact(uid),s=session(5,1000),p=pkg(500,s);
        booking.create(uid,"child-protected",request(c,line("PACKAGE",p,1)));
        insertUser("admin","secret123","ADMIN",true);String token=loginToken("admin","secret123");
        mockMvc.perform(delete("/api/admin/sessions/"+s).header("Authorization",bearer(token))).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(40902));
    }
    private void assertNoOrders(){for(String table:List.of("orders","order_items","order_allocations","order_events","outbox_events"))assertThat(count(table)).as(table).isZero();}
    private <T> List<T> parallel(List<Callable<T>> calls)throws Exception {
        ExecutorService pool=Executors.newFixedThreadPool(calls.size());CountDownLatch ready=new CountDownLatch(calls.size()),start=new CountDownLatch(1);List<Future<T>> futures=new ArrayList<>();
        try{for(Callable<T> c:calls)futures.add(pool.submit(()->{ready.countDown();if(!start.await(15,TimeUnit.SECONDS))throw new IllegalStateException("start timeout");return c.call();}));
            assertThat(ready.await(15,TimeUnit.SECONDS)).isTrue();start.countDown();List<T> results=new ArrayList<>();for(Future<T> future:futures)results.add(future.get(45,TimeUnit.SECONDS));return results;
        }finally{start.countDown();pool.shutdown();assertThat(pool.awaitTermination(60,TimeUnit.SECONDS)).isTrue();}
    }
}
