package com.citygo;
import com.citygo.booking.service.BookingService;
import com.citygo.payment.service.PaymentService;
import com.citygo.notification.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import java.time.*;
import static org.assertj.core.api.Assertions.*;
@Import(PaymentLifecycleIntegrationTest.TimeConfig.class) class ReminderIntegrationTest extends P2Fixtures {
 @Autowired PaymentLifecycleIntegrationTest.MutableTime time;@Autowired BookingService booking;@Autowired PaymentService payments;@Autowired OutboxWorker worker;@Autowired WsTicketService tickets;
 long uid,order;String token;
 @BeforeEach void fixture()throws Exception{time.current=Instant.now();uid=insertUser("reminder","secret123","USER",true);long s=session(2,1000);order=booking.create(uid,"reminder-order-key",request(contact(uid),line("SESSION",s,1))).order().id();token=loginToken("reminder","secret123");}
 @Test void reminderBatchIsUniqueAndDurable(){payments.pay(uid,order,"SUCCESS");payments.confirm(0L,order);worker.reminders();assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM outbox_events WHERE event_type='ACTIVITY_REMINDER'",Integer.class)).isZero();time.advance(Duration.ofDays(10).minusMinutes(30));worker.reminders();worker.reminders();assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM outbox_events WHERE event_type='ACTIVITY_REMINDER'",Integer.class)).isEqualTo(1);worker.drain();worker.drain();assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM notifications WHERE event_type='ACTIVITY_REMINDER'",Integer.class)).isEqualTo(1);}
 @Test void openConnectionIdentityExpiresAndDisabledUserCannotConsumeTicket(){var user=new com.citygo.auth.security.LoginUser(uid,"reminder","USER","reminder");var a=tickets.consume(tickets.issue(user,bearer(token)));assertThat(tickets.valid(a)).isTrue();time.advance(Duration.ofMinutes(31));assertThat(tickets.valid(a)).isFalse();time.current=Instant.now();var ticket=tickets.issue(user,bearer(token));jdbcTemplate.update("UPDATE users SET enabled=0 WHERE id=?",uid);assertThatThrownBy(()->tickets.consume(ticket)).isInstanceOf(com.citygo.common.exception.BizException.class);}
}
