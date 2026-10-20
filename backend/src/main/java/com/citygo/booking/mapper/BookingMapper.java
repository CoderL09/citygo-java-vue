package com.citygo.booking.mapper;
import com.citygo.booking.entity.*;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;
import java.util.List;
@Mapper
public interface BookingMapper {
    @Select("SELECT * FROM orders WHERE user_id=#{uid} AND idempotency_key=#{key}") BookingOrder idempotent(@Param("uid") Long uid,@Param("key") String key);
    @Select("SELECT * FROM orders WHERE id=#{id} AND user_id=#{uid}") BookingOrder own(@Param("uid") Long uid,@Param("id") Long id);
    @Select("SELECT * FROM orders WHERE user_id=#{uid} ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}") List<BookingOrder> page(@Param("uid") Long uid,@Param("offset") int offset,@Param("limit") int limit);
    @Select("SELECT COUNT(*) FROM orders WHERE user_id=#{uid}") long count(Long uid);
    @Select("SELECT * FROM order_items WHERE order_id=#{id} ORDER BY id") List<OrderItem> items(Long id);
    @Insert("INSERT INTO orders(order_no,user_id,total_cents,status,idempotency_key,request_digest,contact_name,contact_phone,remark,expire_at,created_at,updated_at) VALUES(#{orderNo},#{userId},#{totalCents},#{status},#{idempotencyKey},#{requestDigest},#{contactName},#{contactPhone},#{remark},#{expireAt},#{createdAt},#{updatedAt})")
    @Options(useGeneratedKeys=true,keyProperty="id") int insert(BookingOrder order);
    @Insert("INSERT INTO order_items(order_id,item_type,item_id,name,session_id,session_snapshot,package_snapshot,quantity,unit_price_cents,line_total_cents,created_at) VALUES(#{orderId},#{itemType},#{itemId},#{name},#{sessionId},#{sessionSnapshot},#{packageSnapshot},#{quantity},#{unitPriceCents},#{lineTotalCents},#{createdAt})") int insertItem(OrderItem item);
    @Insert("INSERT INTO order_allocations(order_id,session_id,quantity,released) VALUES(#{orderId},#{sessionId},#{quantity},0)")
    int allocate(@Param("orderId") Long orderId,@Param("sessionId") Long sessionId,@Param("quantity") int quantity);
    @Insert("INSERT INTO order_events(order_id,from_status,to_status,actor_type,actor_id,reason,created_at) VALUES(#{orderId},'CREATED','PENDING_PAYMENT','USER',#{uid},'创建预约',#{now})")
    int event(@Param("orderId") Long orderId,@Param("uid") Long uid,@Param("now") LocalDateTime now);
    @Insert("INSERT INTO outbox_events(event_type,payload,biz_key,status,created_at,updated_at) VALUES('ORDER_CREATED',#{payload},#{bizKey},'PENDING',#{now},#{now})")
    int outbox(@Param("payload") String payload,@Param("bizKey") String bizKey,@Param("now") LocalDateTime now);
}
