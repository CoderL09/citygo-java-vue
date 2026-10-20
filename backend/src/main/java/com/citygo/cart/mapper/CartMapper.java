package com.citygo.cart.mapper;
import com.citygo.cart.entity.CartItem;
import org.apache.ibatis.annotations.*;
import java.util.List;
@Mapper
public interface CartMapper {
    @Select("SELECT * FROM cart_items WHERE user_id=#{uid} ORDER BY id") List<CartItem> list(Long uid);
    @Select("SELECT * FROM cart_items WHERE id=#{id} AND user_id=#{uid}") CartItem find(@Param("uid") Long uid,@Param("id") Long id);
    @Select("SELECT * FROM cart_items WHERE user_id=#{uid} AND item_type=#{type} AND item_id=#{itemId}") CartItem product(@Param("uid") Long uid,@Param("type") String type,@Param("itemId") Long itemId);
    @Insert("INSERT INTO cart_items(user_id,item_type,item_id,quantity,selected,created_at,updated_at) VALUES(#{userId},#{itemType},#{itemId},#{quantity},#{selected},#{createdAt},#{updatedAt})")
    @Options(useGeneratedKeys=true,keyProperty="id") int insert(CartItem c);
    @Update("UPDATE cart_items SET quantity=#{quantity},selected=#{selected},updated_at=#{updatedAt} WHERE id=#{id} AND user_id=#{userId}") int update(CartItem c);
    @Delete("DELETE FROM cart_items WHERE id=#{id} AND user_id=#{uid}") int delete(@Param("uid") Long uid,@Param("id") Long id);
    @Delete("DELETE FROM cart_items WHERE user_id=#{uid}") int clear(Long uid);
    @Delete("DELETE FROM cart_items WHERE user_id=#{uid} AND item_type=#{type} AND item_id=#{itemId} AND quantity=#{quantity}")
    int consume(@Param("uid") Long uid,@Param("type") String type,@Param("itemId") Long itemId,@Param("quantity") int quantity);
}
