package com.citygo.catalog.mapper;

import com.citygo.catalog.entity.Session;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SessionMapper {
    @org.apache.ibatis.annotations.Select("SELECT * FROM sessions WHERE id=#{id} FOR UPDATE")
    Session lockById(Long id);
    @org.apache.ibatis.annotations.Select("SELECT * FROM sessions WHERE id=#{id} FOR SHARE")
    Session shareById(Long id);


    int insert(Session session);

    Session findById(@Param("id") Long id);

    int update(@Param("id") Long id, @Param("startTime") LocalDateTime startTime,
               @Param("endTime") LocalDateTime endTime, @Param("bookingDeadline") LocalDateTime bookingDeadline,
               @Param("priceCents") Integer priceCents, @Param("capacity") Integer capacity,
               @Param("available") Integer available, @Param("updatedAt") LocalDateTime updatedAt);

    int updateStatus(@Param("id") Long id, @Param("status") String status,
                     @Param("updatedAt") LocalDateTime updatedAt);

    int deleteById(@Param("id") Long id);

    List<Session> findByActivityId(@Param("activityId") Long activityId, @Param("status") String status);

    /**
     * 有效订单引用数：待支付/待确认/已确认/退款中均占名额且不可改（实施书 §7）。
     * P1 阶段订单尚无数据流，校验逻辑先行，P2/P3 生效。
     */
    @org.apache.ibatis.annotations.Select("SELECT (SELECT COUNT(*) FROM package_items WHERE session_id=#{id}) + (SELECT COUNT(*) FROM order_allocations WHERE session_id=#{id}) + (SELECT COUNT(*) FROM order_items WHERE session_id=#{id}) + (SELECT COUNT(*) FROM order_reschedules WHERE from_session_id=#{id} OR to_session_id=#{id}) + (SELECT COUNT(*) FROM order_fulfillments WHERE session_id=#{id})")
    long countAnyRefs(Long id);

    long countValidOrderRefs(@Param("sessionId") Long sessionId);
}
