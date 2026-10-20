package com.citygo.catalog.mapper;

import com.citygo.catalog.entity.Activity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ActivityMapper {
    @org.apache.ibatis.annotations.Select("SELECT * FROM activities WHERE id=#{id} FOR UPDATE")
    Activity lockById(Long id);
    @org.apache.ibatis.annotations.Select("SELECT * FROM activities WHERE id=#{id} FOR SHARE")
    Activity shareById(Long id);


    int insert(Activity activity);

    Activity findById(@Param("id") Long id);

    int update(@Param("id") Long id, @Param("categoryId") Long categoryId, @Param("name") String name,
               @Param("description") String description, @Param("address") String address,
               @Param("updatedAt") LocalDateTime updatedAt);

    int updateStatus(@Param("id") Long id, @Param("status") String status,
                     @Param("updatedAt") LocalDateTime updatedAt);

    int deleteById(@Param("id") Long id);

    List<Activity> page(@Param("categoryId") Long categoryId, @Param("status") String status,
                        @Param("keyword") String keyword, @Param("offset") int offset,
                        @Param("pageSize") int pageSize);

    long count(@Param("categoryId") Long categoryId, @Param("status") String status,
               @Param("keyword") String keyword);
}
