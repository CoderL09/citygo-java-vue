package com.citygo.payment.mapper;
import com.citygo.booking.entity.BookingOrder;
import com.citygo.payment.entity.*;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;
import java.util.*;
@Mapper public interface LifecycleMapper {
    @Select("SELECT * FROM orders WHERE id=#{id} FOR UPDATE") BookingOrder lockOrder(Long id);
    @Select("SELECT * FROM orders WHERE id=#{id}") BookingOrder order(Long id);
    @Select("SELECT close_intent FROM orders WHERE id=#{id}") String closeIntent(Long id);
    @Update("UPDATE orders SET close_intent=#{intent},close_retry_at=#{retry} WHERE id=#{id}") int setCloseIntent(@Param("id")Long id,@Param("intent")String intent,@Param("retry")LocalDateTime retry);
    @Update("UPDATE orders SET status=#{to},updated_at=#{now},paid_at=CASE WHEN #{to}='PENDING_CONFIRMATION' THEN #{occurred} ELSE paid_at END,confirmed_at=CASE WHEN #{to}='CONFIRMED' THEN #{now} ELSE confirmed_at END,completed_at=CASE WHEN #{to}='COMPLETED' THEN #{now} ELSE completed_at END,cancelled_at=CASE WHEN #{to}='CANCELLED' THEN #{now} ELSE cancelled_at END,closed_at=CASE WHEN #{to}='CLOSED' THEN #{now} ELSE closed_at END,refunded_at=CASE WHEN #{to}='REFUNDED' THEN #{now} ELSE refunded_at END WHERE id=#{id} AND status=#{from}")
    int transition(@Param("id")Long id,@Param("from")String from,@Param("to")String to,@Param("now")LocalDateTime now,@Param("occurred")LocalDateTime occurred);
    @Insert("INSERT INTO order_events(order_id,from_status,to_status,actor_type,actor_id,reason,created_at) VALUES(#{id},#{from},#{to},#{actor},#{actorId},#{reason},#{now})") int event(@Param("id")Long id,@Param("from")String from,@Param("to")String to,@Param("actor")String actor,@Param("actorId")Long actorId,@Param("reason")String reason,@Param("now")LocalDateTime now);
    @Select("SELECT session_id,quantity FROM order_allocations WHERE order_id=#{id} AND released=0 ORDER BY session_id FOR UPDATE") List<Map<String,Object>> allocations(Long id);
    @Select("SELECT id FROM sessions WHERE id=#{id} FOR UPDATE") Long lockSession(Long id);
    @Update("UPDATE sessions SET available=available+#{qty},updated_at=#{now} WHERE id=#{id} AND available+#{qty}<=capacity") int release(@Param("id")Long id,@Param("qty")int qty,@Param("now")LocalDateTime now);
    @Update("UPDATE order_allocations SET released=1,released_at=#{now} WHERE order_id=#{id} AND released=0") int released(@Param("id")Long id,@Param("now")LocalDateTime now);
    @Select("SELECT * FROM payment_attempts WHERE order_id=#{id} ORDER BY id") List<PaymentAttempt> payments(Long id);
    @Select("SELECT * FROM payment_attempts WHERE request_no=#{no}") PaymentAttempt payment(String no);
    @Select("SELECT * FROM payment_attempts WHERE id=#{id} FOR UPDATE") PaymentAttempt lockPayment(Long id);
    @Insert("INSERT INTO payment_attempts(request_no,order_id,channel,amount_cents,status,created_at,updated_at) VALUES(#{requestNo},#{orderId},#{channel},#{amountCents},'PENDING',#{createdAt},#{updatedAt})") @Options(useGeneratedKeys=true,keyProperty="id") int insertPayment(PaymentAttempt p);
    @Update("UPDATE payment_attempts SET status=#{status},external_trade_no=#{external},paid_at=#{paid},updated_at=#{now},next_check_at=#{next} WHERE id=#{id}") int paymentResult(@Param("id")Long id,@Param("status")String status,@Param("external")String external,@Param("paid")LocalDateTime paid,@Param("now")LocalDateTime now,@Param("next")LocalDateTime next);
    @Select("SELECT * FROM refunds WHERE payment_attempt_id=#{id}") RefundRecord paymentRefund(Long id);
    @Select("SELECT * FROM refunds WHERE id=#{id}") RefundRecord refund(Long id);
    @Select("SELECT * FROM refunds WHERE refund_no=#{no}") RefundRecord refundNo(String no);
    @Select("SELECT * FROM refunds WHERE id=#{id} FOR UPDATE") RefundRecord lockRefund(Long id);
    @Select("SELECT * FROM refunds WHERE order_id=#{id} ORDER BY id") List<RefundRecord> refunds(Long id);
    @Insert("INSERT INTO refunds(refund_no,payment_attempt_id,order_id,amount_cents,status,reason,requested_by,compensation,created_at,updated_at) VALUES(#{refundNo},#{paymentAttemptId},#{orderId},#{amountCents},'PENDING',#{reason},#{requestedBy},#{compensation},#{createdAt},#{updatedAt})") @Options(useGeneratedKeys=true,keyProperty="id") int insertRefund(RefundRecord r);
    @Update("UPDATE refunds SET status=#{status},fail_reason=#{failure},success_at=#{success},updated_at=#{now},next_check_at=#{next} WHERE id=#{id}") int refundResult(@Param("id")Long id,@Param("status")String status,@Param("failure")String failure,@Param("success")LocalDateTime success,@Param("now")LocalDateTime now,@Param("next")LocalDateTime next);
    @Insert("INSERT INTO payment_anomalies(payment_attempt_id,order_id,reason,resolved,created_at) VALUES(#{pid},#{oid},#{reason},0,#{now}) ON DUPLICATE KEY UPDATE payment_attempt_id=payment_attempt_id") int anomaly(@Param("pid")Long pid,@Param("oid")Long oid,@Param("reason")String reason,@Param("now")LocalDateTime now);
    @Update("UPDATE payment_anomalies SET resolved=1 WHERE payment_attempt_id=#{id}") int resolveAnomaly(Long id);
    @Insert("INSERT INTO outbox_events(event_type,payload,biz_key,status,created_at,updated_at) VALUES('ORDER_STATUS',#{payload},#{bizKey},'PENDING',#{now},#{now})")
    int outboxStatus(@Param("payload")String payload,@Param("bizKey")String bizKey,@Param("now")LocalDateTime now);
    @Select("SELECT * FROM refunds ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}") List<RefundRecord> adminRefunds(@Param("offset")int offset,@Param("limit")int limit);
    @Select("SELECT COUNT(*) FROM refunds")long refundCount();
    @Select("SELECT * FROM payment_anomalies ORDER BY id DESC LIMIT 100")List<Map<String,Object>> anomalies();
    @Select("SELECT id FROM orders WHERE status='PENDING_PAYMENT' AND (expire_at<=#{now} OR close_intent IS NOT NULL) AND (close_retry_at IS NULL OR close_retry_at<=#{now}) ORDER BY id LIMIT 100") List<Long> dueOrders(LocalDateTime now);
    @Select("SELECT * FROM payment_attempts WHERE status IN ('PENDING','UNKNOWN','CLOSED') AND (next_check_at IS NULL OR next_check_at<=#{now}) ORDER BY COALESCE(next_check_at,created_at),id LIMIT 100") List<PaymentAttempt> duePayments(LocalDateTime now);
    @Select("SELECT * FROM refunds WHERE status IN ('PENDING','PROCESSING','FAILED') AND (next_check_at IS NULL OR next_check_at<=#{now}) ORDER BY COALESCE(next_check_at,created_at),id LIMIT 100") List<RefundRecord> dueRefunds(LocalDateTime now);
    @Select("<script>SELECT * FROM orders WHERE 1=1 <if test='status!=null'>AND status=#{status}</if> <if test='keyword!=null'>AND order_no LIKE CONCAT('%',#{keyword},'%')</if> ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}</script>") List<BookingOrder> adminPage(@Param("status")String status,@Param("keyword")String keyword,@Param("offset")int offset,@Param("limit")int limit);
    @Select("<script>SELECT COUNT(*) FROM orders WHERE 1=1 <if test='status!=null'>AND status=#{status}</if> <if test='keyword!=null'>AND order_no LIKE CONCAT('%',#{keyword},'%')</if></script>") long adminCount(@Param("status")String status,@Param("keyword")String keyword);
    @Insert("INSERT INTO job_failures(job_name,target_id,error_code,created_at) VALUES(#{job},#{id},#{error},#{now})") int jobFailure(@Param("job")String job,@Param("id")Long id,@Param("error")String error,@Param("now")LocalDateTime now);
}
