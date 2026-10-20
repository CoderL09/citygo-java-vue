package com.citygo.settings.mapper;

import com.citygo.settings.entity.PlatformSetting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface PlatformSettingMapper {

    PlatformSetting findByKey(@Param("settingKey") String settingKey);

    int updateByKey(@Param("settingKey") String settingKey, @Param("settingValue") String settingValue,
                    @Param("updatedBy") Long updatedBy, @Param("updatedAt") LocalDateTime updatedAt);
}
