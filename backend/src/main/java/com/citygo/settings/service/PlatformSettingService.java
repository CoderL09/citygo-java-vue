package com.citygo.settings.service;

import com.citygo.settings.entity.PlatformSetting;
import com.citygo.settings.mapper.PlatformSettingMapper;
import com.citygo.common.api.ErrorCode;
import com.citygo.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 平台接单开关（accepting_orders）。暂停时新下单被拒（P2 下单侧校验），已有订单继续处理。
 */
@Service
@RequiredArgsConstructor
public class PlatformSettingService {

    public static final String KEY_ACCEPTING_ORDERS = "accepting_orders";

    private final PlatformSettingMapper platformSettingMapper;
    private final Clock clock;

    public boolean acceptingOrders() {
        PlatformSetting setting = platformSettingMapper.findByKey(KEY_ACCEPTING_ORDERS);
        if (setting == null) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "平台开关配置缺失");
        }
        return "true".equalsIgnoreCase(setting.getSettingValue());
    }

    @Transactional
    public boolean setAcceptingOrders(boolean acceptingOrders, Long operatorId) {
        platformSettingMapper.updateByKey(KEY_ACCEPTING_ORDERS, String.valueOf(acceptingOrders),
                operatorId, LocalDateTime.now(clock));
        return acceptingOrders;
    }
}
