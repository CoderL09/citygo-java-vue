package com.citygo;
import com.citygo.payment.service.*;
import com.citygo.payment.gateway.*;
import com.citygo.payment.gateway.PaymentGateway.*;
import com.citygo.payment.mapper.LifecycleMapper;
import com.citygo.booking.service.BookingService;
import com.citygo.common.exception.BizException;
import com.citygo.config.PaymentEnvironmentGuard;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.mock.env.MockEnvironment;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(PaymentLifecycleIntegrationTest.TimeConfig.class)
class PaymentLifecycleIntegrationTest extends P2Fixtures {
    static class MutableTime extends Clock {
        volatile Instant current=Instant.now();public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId z){return this;}public Instant instant(){return current;}void advance(Duration d){current=current.plus(d);}
    }
    @TestConfiguration static class TimeConfig {@Bean @Primary MutableTime lifecycleTime(){return new MutableTime();}}
    @Autowired MutableTime time;@Autowired PaymentService payments;@Autowired BookingService booking;@Autowired LifecycleMapper m;@Autowired MockCallbackService callbacks;
    @MockitoSpyBean MockPaymentGateway gateway;
    long uid,contact,session,order;
    @BeforeEach void fixture(){time.current=Instant.now();uid=insertUser("payer","secret123","USER",true);contact=contact(uid);session=session(2,1000);order=booking.create(uid,"payment-fixture-key",request(contact,line("SESSION",session,1))).order().id();}
    @Test void payConfirmRefundIsIdempotentAndReleasesExactlyOnce(){
        payments.pay(uid,order,"SUCCESS");payments.pay(uid,order,"SUCCESS");payments.confirm(99L,order);payments.confirm(99L,order);
        assertThat(m.order(order).getStatus()).isEqualTo("CONFIRMED");assertThat(stock(session)).isEqualTo(1);
        var refund=payments.refund(uid,order,"计划变化");payments.refund(uid,order,"重复请求");payments.retryRefund(refund.getId());
        assertThat(m.order(order).getStatus()).isEqualTo("REFUNDED");assertThat(stock(session)).isEqualTo(2);assertThat(count("refunds")).isEqualTo(1);assertThat(count("mock_gateway_refunds")).isEqualTo(1);
        verify(gateway,times(1)).refund(any(),anyString());
    }
    @Test void failedPaymentCanRetryButUnknownPaymentUsesOriginalRequest(){
        payments.pay(uid,order,"FAIL");assertThat(m.order(order).getStatus()).isEqualTo("PENDING_PAYMENT");
        payments.pay(uid,order,"SUCCESS");assertThat(m.payments(order)).hasSize(2);assertThat(m.order(order).getStatus()).isEqualTo("PENDING_CONFIRMATION");
    }
    @Test void lostNotificationRecoveredByReconciliation(){
        payments.pay(uid,order,"DELAYED");assertThat(m.order(order).getStatus()).isEqualTo("PENDING_PAYMENT");
        time.advance(Duration.ofSeconds(61));payments.reconcilePayments();payments.reconcilePayments();
        assertThat(m.order(order).getStatus()).isEqualTo("PENDING_CONFIRMATION");assertThat(m.payments(order)).hasSize(1);assertThat(stock(session)).isEqualTo(1);
    }
    @Test void timeoutAndRepeatedExpiryReleaseOnlyOnce(){
        payments.pay(uid,order,"TIMEOUT");time.advance(Duration.ofMinutes(16));payments.closeExpired();payments.closeExpired();
        assertThat(m.order(order).getStatus()).isEqualTo("CLOSED");assertThat(stock(session)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("SELECT released FROM order_allocations WHERE order_id=?",Boolean.class,order)).isTrue();
    }
    @Test void cancelledThenLatePaymentIsRecordedAndCompensatedWithoutReopening(){
        payments.pay(uid,order,"TIMEOUT");var p=m.payments(order).getFirst();payments.cancel(uid,order);
        Result late=new Result(p.getRequestNo(),m.order(order).getOrderNo(),1000,"SUCCESS","LATE_TRADE",LocalDateTime.now(time));
        callbacks.receive(late,callbacks.signature(late));callbacks.receive(late,callbacks.signature(late));
        assertThat(m.order(order).getStatus()).isEqualTo("CANCELLED");assertThat(stock(session)).isEqualTo(2);assertThat(count("refunds")).isEqualTo(1);
        assertThat(m.refunds(order).getFirst().getCompensation()).isTrue();assertThat(m.refunds(order).getFirst().getStatus()).isEqualTo("SUCCESS");
        assertThat(jdbcTemplate.queryForObject("SELECT resolved FROM payment_anomalies WHERE order_id=?",Boolean.class,order)).isTrue();
    }
    @Test void paymentMadeAfterExpiryClosesAndRefundsRatherThanAllocating(){
        payments.pay(uid,order,"TIMEOUT");var p=m.payments(order).getFirst();time.advance(Duration.ofMinutes(16));
        Result late=new Result(p.getRequestNo(),m.order(order).getOrderNo(),1000,"SUCCESS","EXPIRED_TRADE",LocalDateTime.now(time));
        callbacks.receive(late,callbacks.signature(late));
        assertThat(m.order(order).getStatus()).isEqualTo("CLOSED");assertThat(stock(session)).isEqualTo(2);assertThat(m.refunds(order).getFirst().getStatus()).isEqualTo("SUCCESS");
    }
    @Test void refundFailureStaysRefundingAndRetryKeepsSameRequest(){
        payments.pay(uid,order,"SUCCESS");doThrow(new IllegalStateException("provider unavailable")).when(gateway).refund(any(),anyString());
        var refund=payments.refund(uid,order,"退款失败测试");assertThat(refund.getStatus()).isEqualTo("FAILED");assertThat(refund.getFailReason()).isNotBlank();
        assertThat(m.order(order).getStatus()).isEqualTo("REFUNDING");assertThat(stock(session)).isEqualTo(1);
        reset(gateway);payments.retryRefund(refund.getId());
        assertThat(m.refunds(order)).hasSize(1);assertThat(m.refunds(order).getFirst().getRefundNo()).isEqualTo(refund.getRefundNo());assertThat(stock(session)).isEqualTo(2);
    }
    @Test void unknownRefundResultReconcilesWithoutRepeatingRelease(){
        payments.pay(uid,order,"SUCCESS");doAnswer(inv->{Result actual=(Result)inv.callRealMethod();return new Result(actual.requestNo(),actual.orderNo(),actual.amountCents(),"UNKNOWN",null,null);}).when(gateway).refund(any(),anyString());
        payments.refund(uid,order,"丢失退款结果");assertThat(stock(session)).isEqualTo(1);assertThat(m.order(order).getStatus()).isEqualTo("REFUNDING");
        time.advance(Duration.ofSeconds(61));payments.reconcileRefunds();payments.reconcileRefunds();
        assertThat(m.order(order).getStatus()).isEqualTo("REFUNDED");assertThat(stock(session)).isEqualTo(2);assertThat(count("mock_gateway_refunds")).isEqualTo(1);
    }
    @Test void rejectsForgedWrongAmountWrongOrderAndReorderedCallbacks()throws Exception{
        payments.pay(uid,order,"TIMEOUT");var p=m.payments(order).getFirst();String no=m.order(order).getOrderNo();
        Result valid=new Result(p.getRequestNo(),no,1000,"SUCCESS","SIGNED_TRADE",LocalDateTime.now(time));
        mockMvc.perform(post("/api/payment-callbacks/mock").header("X-Mock-Signature","bad").contentType(APPLICATION_JSON).content(objectMapper.writeValueAsString(valid))).andExpect(status().isForbidden());
        Result wrong=new Result(p.getRequestNo(),no,1,"SUCCESS","WRONG",LocalDateTime.now(time));
        mockMvc.perform(post("/api/payment-callbacks/mock").header("X-Mock-Signature",callbacks.signature(wrong)).contentType(APPLICATION_JSON).content(objectMapper.writeValueAsString(wrong))).andExpect(status().isConflict());
        Result wrongOrder=new Result(p.getRequestNo(),"wrong-order",1000,"SUCCESS","WRONG_ORDER",LocalDateTime.now(time));
        assertThatThrownBy(()->callbacks.receive(wrongOrder,callbacks.signature(wrongOrder))).isInstanceOf(BizException.class);
        callbacks.receive(valid,callbacks.signature(valid));
        Result oldFailure=new Result(p.getRequestNo(),no,1000,"FAILED",null,null);callbacks.receive(oldFailure,callbacks.signature(oldFailure));
        assertThat(m.order(order).getStatus()).isEqualTo("PENDING_CONFIRMATION");assertThat(m.payment(p.getRequestNo()).getStatus()).isEqualTo("SUCCESS");
    }
    @Test void confirmationAndCompletionRespectStateAndLatestStart_andCompletionDoesNotRelease(){
        assertThatThrownBy(()->payments.confirm(99L,order)).isInstanceOf(BizException.class);
        payments.pay(uid,order,"SUCCESS");payments.confirm(99L,order);
        assertThatThrownBy(()->payments.complete(99L,order)).isInstanceOf(BizException.class);
        time.advance(Duration.ofDays(11));payments.complete(99L,order);payments.complete(99L,order);
        assertThat(m.order(order).getStatus()).isEqualTo("COMPLETED");assertThat(stock(session)).isEqualTo(1);
        assertThatThrownBy(()->payments.refund(uid,order,"已完成退款拒绝")).isInstanceOf(BizException.class);
        assertThat(count("order_events")).isEqualTo(4);
    }
    @Test void paidOrderCannotCancel_andEarliestStartBlocksUserRefund(){
        payments.pay(uid,order,"SUCCESS");assertThatThrownBy(()->payments.cancel(uid,order)).isInstanceOf(BizException.class);
        time.advance(Duration.ofDays(11));assertThatThrownBy(()->payments.refund(uid,order,"已开始")).isInstanceOf(BizException.class);assertThat(stock(session)).isEqualTo(1);
    }
    @Test void rejectRefundsAndRepeatedRejectHasNoExtraSideEffects(){
        payments.pay(uid,order,"SUCCESS");payments.reject(99L,order,"无法接待");payments.reject(99L,order,"重复");
        assertThat(m.order(order).getStatus()).isEqualTo("REFUNDED");assertThat(stock(session)).isEqualTo(2);assertThat(count("refunds")).isEqualTo(1);
    }
    @Test void payAndCancelRaceConvergesWithoutDoubleRelease()throws Exception{
        ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);
        try{Future<?> a=pool.submit(()->{await(start);try{payments.pay(uid,order,"SUCCESS");}catch(BizException expected){assertThat(expected.getErrorCode().getCode()).isIn(40907,40908);}});
            Future<?> b=pool.submit(()->{await(start);try{payments.cancel(uid,order);}catch(BizException expected){assertThat(expected.getErrorCode().getCode()).isEqualTo(40908);}});start.countDown();a.get(20,TimeUnit.SECONDS);b.get(20,TimeUnit.SECONDS);
        }finally{pool.shutdown();assertThat(pool.awaitTermination(30,TimeUnit.SECONDS)).isTrue();}
        String state=m.order(order).getStatus();assertThat(state).isIn("CANCELLED","PENDING_CONFIRMATION");assertThat(stock(session)).isEqualTo("CANCELLED".equals(state)?2:1);
    }
    @Test void concurrentFullRefundOnlyCreatesOneProviderRefund()throws Exception{
        payments.pay(uid,order,"SUCCESS");ExecutorService pool=Executors.newFixedThreadPool(8);
        try{List<Callable<Long>> calls=new ArrayList<>();for(int i=0;i<10;i++)calls.add(()->payments.refund(uid,order,"并发退款").getId());for(Future<Long> f:pool.invokeAll(calls,30,TimeUnit.SECONDS))assertThat(f.get()).isPositive();}
        finally{pool.shutdown();assertThat(pool.awaitTermination(30,TimeUnit.SECONDS)).isTrue();}
        assertThat(count("refunds")).isEqualTo(1);assertThat(count("mock_gateway_refunds")).isEqualTo(1);assertThat(stock(session)).isEqualTo(2);
    }
    @Test void ownedMoneyEndpointsAndAdminActionsAreProtected()throws Exception{
        insertUser("intruder3","secret123","USER",true);String token=loginToken("intruder3","secret123");
        mockMvc.perform(get("/api/orders/"+order+"/payments").header("Authorization",bearer(token))).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/orders/"+order+"/payment").header("Authorization",bearer(token)).contentType(APPLICATION_JSON).content("{\"scenario\":\"SUCCESS\"}")).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/admin/orders/"+order+"/confirm").header("Authorization",bearer(token))).andExpect(status().isForbidden());
    }
    @Test void mockIsForbiddenForProductionAndOtherProfiles(){
        MockEnvironment prod=new MockEnvironment();prod.setActiveProfiles("prod");assertThatThrownBy(()->new PaymentEnvironmentGuard(prod,true)).isInstanceOf(IllegalStateException.class);
        MockEnvironment staging=new MockEnvironment();staging.setActiveProfiles("staging");assertThatThrownBy(()->new PaymentEnvironmentGuard(staging,true)).isInstanceOf(IllegalStateException.class);
    }
    @Test void packageCompletionWaitsForLatestChildStart(){
        payments.cancel(uid,order);
        long b=session(2,800);LocalDateTime future=LocalDateTime.now(time).plusDays(12);
        jdbcTemplate.update("UPDATE sessions SET start_time=?,end_time=?,booking_deadline=? WHERE id=?",future,future.plusHours(2),future.minusDays(1),b);
        long pkg=pkg(1500,session,b),packOrder=booking.create(uid,"multi-start-boundary",request(contact,line("PACKAGE",pkg,1))).order().id();
        payments.pay(uid,packOrder,"SUCCESS");payments.confirm(99L,packOrder);time.advance(Duration.ofDays(11));
        assertThatThrownBy(()->payments.complete(99L,packOrder)).isInstanceOf(BizException.class);
        assertThatThrownBy(()->payments.refund(uid,packOrder,"最早场次已开始")).isInstanceOf(BizException.class);
        time.advance(Duration.ofDays(2));payments.complete(99L,packOrder);
        assertThat(m.order(packOrder).getStatus()).isEqualTo("COMPLETED");assertThat(stock(session)).isEqualTo(1);assertThat(stock(b)).isEqualTo(1);
    }
    @Test void failedLatePaymentCompensationIsVisibleAndRetryNeverReopensBooking(){
        payments.pay(uid,order,"TIMEOUT");var p=m.payments(order).getFirst();payments.cancel(uid,order);
        doThrow(new IllegalStateException("refund provider unavailable")).when(gateway).refund(any(),anyString());
        Result late=new Result(p.getRequestNo(),m.order(order).getOrderNo(),1000,"SUCCESS","FAILED_COMPENSATION",LocalDateTime.now(time));
        callbacks.receive(late,callbacks.signature(late));var refund=m.refunds(order).getFirst();
        assertThat(refund.getStatus()).isEqualTo("FAILED");assertThat(refund.getCompensation()).isTrue();assertThat(stock(session)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("SELECT resolved FROM payment_anomalies WHERE order_id=?",Boolean.class,order)).isFalse();
        reset(gateway);payments.retryRefund(refund.getId());
        assertThat(m.order(order).getStatus()).isEqualTo("CANCELLED");assertThat(stock(session)).isEqualTo(2);assertThat(m.refunds(order)).hasSize(1);
    }
    private void await(CountDownLatch latch){try{if(!latch.await(10,TimeUnit.SECONDS))throw new IllegalStateException("start timeout");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}}
}
