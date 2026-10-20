package com.citygo;
import com.citygo.booking.service.BookingService;
import com.citygo.contact.service.ContactService;
import com.citygo.common.exception.BizException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;

class PublicationRaceIntegrationTest extends P2Fixtures {
    @Autowired BookingService booking;
    @Autowired ContactService contacts;
    @Autowired PlatformTransactionManager transactions;
    @Test void activityOffSaleCommitWinsAgainstBlockedCheckout()throws Exception{publicationRace(false);}
    @Test void pauseCommitWinsAgainstBlockedCheckout()throws Exception{publicationRace(true);}
    private void publicationRace(boolean platform)throws Exception{
        long uid=insertUser("racing","secret123","USER",true),c=contact(uid),s=session(5,1000);
        long activity=jdbcTemplate.queryForObject("SELECT activity_id FROM sessions WHERE id=?",Long.class,s);
        ExecutorService pool=Executors.newSingleThreadExecutor();CountDownLatch started=new CountDownLatch(1);
        AtomicReference<Future<?>> future=new AtomicReference<>();
        try{
            new TransactionTemplate(transactions).executeWithoutResult(status->{
                // Hold the real MySQL publication row until the competing checkout has tried to proceed.
                if(platform)jdbcTemplate.update("UPDATE platform_settings SET setting_value='false' WHERE setting_key='accepting_orders'");
                else jdbcTemplate.update("UPDATE activities SET status='OFF_SALE' WHERE id=?",activity);
                future.set(pool.submit(()->{started.countDown();return booking.create(uid,"publication-race",request(c,line("SESSION",s,1)));}));
                try{assertThat(started.await(5,TimeUnit.SECONDS)).isTrue();}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}
                assertThatThrownBy(()->future.get().get(200,TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            });
            assertThatThrownBy(()->future.get().get(10,TimeUnit.SECONDS)).isInstanceOf(ExecutionException.class).hasCauseInstanceOf(BizException.class);
            assertThat(stock(s)).isEqualTo(5);assertThat(count("orders")).isZero();assertThat(count("order_allocations")).isZero();
        }finally{pool.shutdown();assertThat(pool.awaitTermination(30,TimeUnit.SECONDS)).isTrue();}
    }
    @Test void priceChangeCommitWinsAndCheckoutUsesCurrentPrice()throws Exception{
        long uid=insertUser("pricechange","secret123","USER",true),c=contact(uid),s=session(5,1000);
        ExecutorService pool=Executors.newSingleThreadExecutor();CountDownLatch started=new CountDownLatch(1);
        AtomicReference<Future<BookingService.Detail>> future=new AtomicReference<>();
        try{
            new TransactionTemplate(transactions).executeWithoutResult(status->{
                jdbcTemplate.update("UPDATE sessions SET price_cents=2400 WHERE id=?",s);
                future.set(pool.submit(()->{started.countDown();return booking.create(uid,"price-race-0001",request(c,line("SESSION",s,1)));}));
                try{assertThat(started.await(5,TimeUnit.SECONDS)).isTrue();}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}
                assertThatThrownBy(()->future.get().get(200,TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            });
            assertThat(future.get().get(10,TimeUnit.SECONDS).order().totalCents()).isEqualTo(2400);
            assertThat(stock(s)).isEqualTo(4);assertThat(count("orders")).isEqualTo(1);
        }finally{pool.shutdown();assertThat(pool.awaitTermination(30,TimeUnit.SECONDS)).isTrue();}
    }
    @Test void concurrentDefaultContactRequestsLeaveExactlyOneDefault()throws Exception{
        long uid=insertUser("defaults","secret123","USER",true);contact(uid);
        ExecutorService pool=Executors.newFixedThreadPool(8);
        try{List<Callable<Long>> tasks=new ArrayList<>();for(int i=0;i<20;i++){String name="联系人"+i;tasks.add(()->contacts.save(uid,null,new ContactService.Request(name,"13800000000",true)).getId());}
            List<Future<Long>> results=pool.invokeAll(tasks,30,TimeUnit.SECONDS);for(Future<Long> f:results)assertThat(f.get()).isPositive();
        }finally{pool.shutdown();assertThat(pool.awaitTermination(30,TimeUnit.SECONDS)).isTrue();}
        assertThat(count("contacts")).isEqualTo(21);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM contacts WHERE user_id=? AND is_default=1",Integer.class,uid)).isEqualTo(1);
    }
    @Test void digestBindsContactRemarkAndNormalizedLineOrder(){
        long uid=insertUser("digest","secret123","USER",true),c=contact(uid),a=session(10,1000),b=session(10,1000);
        var first=booking.create(uid,"normalized-request",request(c,line("SESSION",b,1),line("SESSION",a,1)));
        var replay=booking.create(uid,"normalized-request",request(c,line("SESSION",a,1),line("SESSION",b,1)));
        assertThat(replay.order().id()).isEqualTo(first.order().id());
        assertThatThrownBy(()->booking.create(uid,"normalized-request",new com.citygo.booking.dto.CheckoutRequest(c,List.of(line("SESSION",a,1),line("SESSION",b,1)),"changed")))
            .isInstanceOfSatisfying(BizException.class,e->assertThat(e.getErrorCode().getCode()).isEqualTo(40913));
        assertThat(stock(a)).isEqualTo(9);assertThat(stock(b)).isEqualTo(9);
    }
}
