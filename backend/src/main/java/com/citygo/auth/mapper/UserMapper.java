package com.citygo.auth.mapper;

import com.citygo.auth.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserMapper {

    int insert(User user);

    User findById(@Param("id") Long id);

    User findByUsername(@Param("username") String username);

    int updateProfile(@Param("id") Long id, @Param("nickname") String nickname,
                      @Param("phone") String phone, @Param("updatedAt") LocalDateTime updatedAt);

    int updateRole(@Param("id") Long id, @Param("role") String role,
                   @Param("updatedAt") LocalDateTime updatedAt);

    int updateEnabled(@Param("id") Long id, @Param("enabled") boolean enabled,
                      @Param("updatedAt") LocalDateTime updatedAt);

    int updatePasswordHash(@Param("id") Long id, @Param("passwordHash") String passwordHash,
                           @Param("updatedAt") LocalDateTime updatedAt);

    long countByRole(@Param("role") String role);

    long countEnabledAdmins();

    List<User> pageStaff(@Param("keyword") String keyword, @Param("offset") int offset,
                         @Param("pageSize") int pageSize);

    long countStaff(@Param("keyword") String keyword);
}
