package com.citygo.notification.mapper;
import com.citygo.notification.entity.*;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;
import java.util.*;
@Mapper public interface NotificationMapper {
    @Select("SELECT id FROM outbox_events WHERE status IN ('PENDING','FAILED') AND (next_retry_at IS NULL OR next_retry_at<=#{now}) ORDER BY COALESCE(next_retry_at,created_at),id LIMIT 100")List<Long> due(LocalDateTime now);
    @Select("SELECT * FROM outbox_events WHERE id=#{id}")OutboxEvent event(Long id);
    @Select("SELECT * FROM outbox_events WHERE id=#{id} FOR UPDATE")OutboxEvent lockEvent(Long id);
    @Update("UPDATE outbox_events SET status='DONE',last_error=NULL,updated_at=#{now} WHERE id=#{id}")int done(@Param("id")Long id,@Param("now")LocalDateTime now);
    @Update("UPDATE outbox_events SET status='FAILED',retry_count=retry_count+1,next_retry_at=#{next},last_error=#{error},updated_at=#{now} WHERE id=#{id} AND status!='DONE'")int fail(@Param("id")Long id,@Param("error")String error,@Param("next")LocalDateTime next,@Param("now")LocalDateTime now);
    @Insert("INSERT INTO outbox_events(event_type,payload,biz_key,status,created_at,updated_at) VALUES(#{type},#{payload},#{key},'PENDING',#{now},#{now}) ON DUPLICATE KEY UPDATE biz_key=biz_key")
    int enqueue(@Param("type")String type,@Param("payload")String payload,@Param("key")String key,@Param("now")LocalDateTime now);
    @Insert("INSERT INTO notifications(event_type,title,content,biz_key,created_at) VALUES(#{eventType},#{title},#{content},#{bizKey},#{createdAt}) ON DUPLICATE KEY UPDATE biz_key=biz_key")int notice(Notice n);
    @Select("SELECT * FROM notifications WHERE biz_key=#{key}")Notice byKey(String key);
    @Insert("INSERT INTO notification_recipients(notification_id,recipient_type,recipient_id,is_read) VALUES(#{nid},#{type},#{uid},0) ON DUPLICATE KEY UPDATE recipient_id=recipient_id")int recipient(@Param("nid")Long nid,@Param("type")String type,@Param("uid")Long uid);
    @Select("SELECT id FROM users WHERE enabled=1 AND role IN ('OPERATOR','ADMIN') ORDER BY id")List<Long> staff();
    @Select("SELECT DISTINCT n.*,r.is_read AS 'read' FROM notifications n JOIN notification_recipients r ON r.notification_id=n.id WHERE r.recipient_id=#{uid} AND n.id>#{after} ORDER BY n.id LIMIT #{limit}")List<Notice> since(@Param("uid")Long uid,@Param("after")Long after,@Param("limit")int limit);
    @Select("SELECT COUNT(DISTINCT notification_id) FROM notification_recipients WHERE recipient_id=#{uid} AND is_read=0")long unread(Long uid);
    @Update("UPDATE notification_recipients SET is_read=1,read_at=#{now} WHERE recipient_id=#{uid} AND notification_id=#{id}")int read(@Param("uid")Long uid,@Param("id")Long id,@Param("now")LocalDateTime now);
    @Update("UPDATE notification_recipients SET is_read=1,read_at=#{now} WHERE recipient_id=#{uid} AND is_read=0")int readAll(@Param("uid")Long uid,@Param("now")LocalDateTime now);
    @Select("SELECT COUNT(*) FROM (SELECT o.id FROM orders o JOIN order_allocations a ON a.order_id=o.id JOIN sessions s ON s.id=a.session_id WHERE o.id=#{id} AND o.status='CONFIRMED' GROUP BY o.id HAVING MIN(s.start_time)>#{now} AND MIN(s.start_time)<=#{until}) eligible")int reminderEligible(@Param("id")Long id,@Param("now")LocalDateTime now,@Param("until")LocalDateTime until);
    @Select("SELECT o.id FROM orders o JOIN order_allocations a ON a.order_id=o.id JOIN sessions s ON s.id=a.session_id WHERE o.status='CONFIRMED' GROUP BY o.id HAVING MIN(s.start_time)>#{now} AND MIN(s.start_time)<=#{until} ORDER BY o.id LIMIT 100")List<Long> reminders(@Param("now")LocalDateTime now,@Param("until")LocalDateTime until);
}
