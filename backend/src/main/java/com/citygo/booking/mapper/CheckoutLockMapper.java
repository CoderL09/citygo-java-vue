package com.citygo.booking.mapper;
import com.citygo.catalog.entity.*;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;
@Mapper
public interface CheckoutLockMapper {
    @Select("SELECT setting_value FROM platform_settings WHERE setting_key='accepting_orders' FOR SHARE") String platform();
    @Select("SELECT * FROM categories WHERE id=#{id} FOR SHARE") Category category(Long id);
    @Select("SELECT * FROM activities WHERE id=#{id} FOR SHARE") Activity activity(Long id);
    @Select("SELECT * FROM sessions WHERE id=#{id} FOR UPDATE") Session session(Long id);
    @Update("UPDATE sessions SET available=available-#{quantity},updated_at=#{now} WHERE id=#{id} AND available>=#{quantity} AND status='ON_SALE' AND booking_deadline>#{now}")
    int reserve(@Param("id") Long id,@Param("quantity") int quantity,@Param("now") LocalDateTime now);
}
