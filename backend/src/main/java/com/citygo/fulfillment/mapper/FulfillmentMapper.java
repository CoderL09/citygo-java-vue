package com.citygo.fulfillment.mapper;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;
import java.util.*;
@Mapper public interface FulfillmentMapper {
 record Current(Long itemId,Long sessionId,String snapshot,int version){}
 record Change(Long id,Long orderId,String requestKey,int expectedVersion,Long fromSessionId,Long toSessionId,int quantity,String beforeSnapshot,String afterSnapshot,Long actorId,LocalDateTime createdAt){}
 record Ticket(String tokenHash,Long orderId,int orderVersion,LocalDateTime expiresAt,LocalDateTime usedAt,Long consumedBy,LocalDateTime createdAt){}
 record Checkin(Long orderId,String source,Long operatorId,LocalDateTime checkedAt){}
 @Select("SELECT fulfillment_version FROM orders WHERE id=#{id}")int version(Long id);
 @Update("UPDATE orders SET fulfillment_version=fulfillment_version+1,updated_at=#{now} WHERE id=#{id}")int advance(@Param("id")Long id,@Param("now")LocalDateTime now);
 @Select("SELECT order_item_id item_id,session_id,snapshot,version FROM order_fulfillments WHERE order_item_id=#{id}")Current current(Long id);
 @Select("SELECT snapshot FROM order_fulfillments WHERE order_item_id=#{id}")String snapshot(Long id);
 @Insert("INSERT INTO order_fulfillments(order_item_id,session_id,snapshot,version,updated_at) VALUES(#{item},#{session},#{snapshot},#{version},#{now}) ON DUPLICATE KEY UPDATE session_id=#{session},snapshot=#{snapshot},version=#{version},updated_at=#{now}")
 int saveCurrent(@Param("item")Long item,@Param("session")Long session,@Param("snapshot")String snapshot,@Param("version")int version,@Param("now")LocalDateTime now);
 @Select("SELECT * FROM order_reschedules WHERE order_id=#{oid} AND request_key=#{key}")Change replay(@Param("oid")Long oid,@Param("key")String key);
 @Select("SELECT * FROM order_reschedules WHERE order_id=#{oid} ORDER BY id DESC LIMIT 100")List<Change> history(Long oid);
 @Insert("INSERT INTO order_reschedules(order_id,request_key,expected_version,from_session_id,to_session_id,quantity,before_snapshot,after_snapshot,actor_id,created_at) VALUES(#{orderId},#{requestKey},#{expectedVersion},#{fromSessionId},#{toSessionId},#{quantity},#{beforeSnapshot},#{afterSnapshot},#{actorId},#{createdAt})")int change(Change change);
 @Update("UPDATE order_allocations SET session_id=#{target} WHERE order_id=#{oid} AND session_id=#{source} AND quantity=#{qty} AND released=0")int move(@Param("oid")Long oid,@Param("source")Long source,@Param("target")Long target,@Param("qty")int qty);
 @Select("SELECT * FROM sessions WHERE activity_id=#{aid} AND id!=#{current} AND status='ON_SALE' AND booking_deadline>#{now} AND price_cents=#{price} AND available>=#{qty} ORDER BY start_time,id LIMIT 100")List<com.citygo.catalog.entity.Session> candidates(@Param("aid")Long aid,@Param("current")Long current,@Param("price")int price,@Param("qty")int qty,@Param("now")LocalDateTime now);
 @Update("UPDATE checkin_tickets SET expires_at=#{now} WHERE order_id=#{oid} AND used_at IS NULL AND expires_at>#{now}")int invalidate(@Param("oid")Long oid,@Param("now")LocalDateTime now);
 @Insert("INSERT INTO checkin_tickets(token_hash,order_id,order_version,expires_at,created_at) VALUES(#{tokenHash},#{orderId},#{orderVersion},#{expiresAt},#{createdAt})")int ticket(Ticket ticket);
 @Select("SELECT * FROM checkin_tickets WHERE token_hash=#{hash}")Ticket ticketByHash(String hash);
 @Select("SELECT * FROM checkin_tickets WHERE token_hash=#{hash} FOR UPDATE")Ticket lockTicket(String hash);
 @Update("UPDATE checkin_tickets SET used_at=#{now},consumed_by=#{actor} WHERE token_hash=#{hash} AND used_at IS NULL")int consume(@Param("hash")String hash,@Param("actor")Long actor,@Param("now")LocalDateTime now);
 @Insert("INSERT INTO order_checkins(order_id,source,operator_id,checked_at) VALUES(#{oid},'MANUAL',#{actor},#{now})")int manualCheckin(@Param("oid")Long oid,@Param("actor")Long actor,@Param("now")LocalDateTime now);
 @Update("UPDATE order_checkins SET source='QR',token_hash=#{hash} WHERE order_id=#{oid}")int markQr(@Param("oid")Long oid,@Param("hash")String hash);
 @Select("SELECT order_id,source,operator_id,checked_at FROM order_checkins WHERE order_id=#{oid}")Checkin checkin(Long oid);
}
