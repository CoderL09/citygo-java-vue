package com.citygo.catalog.mapper;

import com.citygo.catalog.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CategoryMapper {
    @org.apache.ibatis.annotations.Select("SELECT * FROM categories WHERE id=#{id} FOR UPDATE")
    Category lockById(Long id);
    @org.apache.ibatis.annotations.Select("SELECT * FROM categories WHERE id=#{id} FOR SHARE")
    Category shareById(Long id);


    int insert(Category category);

    Category findById(@Param("id") Long id);

    int update(@Param("id") Long id, @Param("name") String name, @Param("sortOrder") Integer sortOrder,
               @Param("updatedAt") LocalDateTime updatedAt);

    int updateStatus(@Param("id") Long id, @Param("status") boolean status,
                     @Param("updatedAt") LocalDateTime updatedAt);

    int deleteById(@Param("id") Long id);

    List<Category> page(@Param("categoryType") String categoryType, @Param("status") Boolean status,
                        @Param("keyword") String keyword, @Param("offset") int offset,
                        @Param("pageSize") int pageSize);

    long count(@Param("categoryType") String categoryType, @Param("status") Boolean status,
               @Param("keyword") String keyword);

    long countActivities(@Param("categoryId") Long categoryId);
}
