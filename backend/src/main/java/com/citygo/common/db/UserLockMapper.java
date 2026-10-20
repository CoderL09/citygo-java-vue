package com.citygo.common.db;
import org.apache.ibatis.annotations.*;
@Mapper
public interface UserLockMapper {
    // Serialize one user's contacts/cart/checkout; no lock spans an external call.
    @Select("SELECT id FROM users WHERE id = #{id} FOR UPDATE")
    Long lock(Long id);
}
