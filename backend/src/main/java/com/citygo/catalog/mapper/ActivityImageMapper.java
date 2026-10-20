package com.citygo.catalog.mapper;

import com.citygo.catalog.entity.ActivityImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ActivityImageMapper {

    int insert(ActivityImage image);

    ActivityImage findById(@Param("id") Long id);

    List<ActivityImage> findByActivityId(@Param("activityId") Long activityId);

    int updateSort(@Param("id") Long id, @Param("sortOrder") Integer sortOrder);

    int deleteById(@Param("id") Long id);

    int deleteByActivityId(@Param("activityId") Long activityId);
}
