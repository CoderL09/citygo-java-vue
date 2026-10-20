package com.citygo.settings.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * platform_settings 表：受控平台设置（如 accepting_orders 接单开关）。
 */
@Data
public class PlatformSetting {

    private Long id;
    private String settingKey;
    private String settingValue;
    private String description;
    private Long updatedBy;
    private LocalDateTime updatedAt;
}
