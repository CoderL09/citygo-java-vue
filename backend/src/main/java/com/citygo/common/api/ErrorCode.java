package com.citygo.common.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    BAD_REQUEST(40000, "请求参数错误"),
    UNAUTHORIZED(40100, "未登录或登录已过期"),
    FORBIDDEN(40300, "无权限执行此操作"),
    NOT_FOUND(40400, "资源不存在"),
    CONFLICT(40900, "请求冲突"),
    TOO_MANY_REQUESTS(42900, "操作过于频繁"),
    INTERNAL_ERROR(50000, "服务内部错误"),
    SERVICE_UNAVAILABLE(50300, "服务暂时不可用，请稍后重试"),

    USER_DISABLED(40101, "账号已被禁用"),
    BAD_CREDENTIALS(40102, "用户名或密码错误"),
    SESSION_EXPIRED(40103, "登录凭证已失效"),

    CATEGORY_HAS_CHILDREN(40901, "分类下存在关联商品，无法删除"),
    ACTIVITY_HAS_VALID_ORDERS(40902, "场次存在有效订单，不允许修改或删除"),
    SESSION_SOLD_OUT(40903, "场次名额不足"),
    SESSION_OFF_SALE(40904, "场次已下架或已过预约截止时间"),
    PACKAGE_OFF_SALE(40905, "套餐已下架"),
    PLATFORM_PAUSED(40906, "平台暂停接单，暂时无法下单"),
    ORDER_NOT_PAYABLE(40907, "订单当前状态不可支付"),
    ORDER_NOT_CANCELLABLE(40908, "订单当前状态不可取消"),
    ORDER_NOT_REFUNDABLE(40909, "订单当前状态不可退款"),
    ORDER_NOT_CONFIRMABLE(40910, "订单当前状态不可确认"),
    ORDER_NOT_COMPLETABLE(40911, "订单不满足核销条件"),
    INVALID_STATE_TRANSITION(40912, "非法的状态流转"),
    IDEMPOTENCY_KEY_CONFLICT(40913, "幂等键已用于不同请求"),
    PAYMENT_AMOUNT_MISMATCH(40914, "支付金额与订单不一致"),
    CART_ITEM_INVALID(40915, "购物车条目无效或已失效"),
    REFUND_AMOUNT_MISMATCH(40916, "退款金额与原支付不一致"),
    REFUND_EXISTS(40917, "退款已存在，请勿重复发起"),
    REFUND_RETRYABLE(40918, "退款失败，可稍后重试");

    private final int code;
    private final String defaultMessage;
}
