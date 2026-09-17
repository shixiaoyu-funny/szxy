package com.shixiaoyu.xiangyueproject.constants;

import java.util.regex.Pattern;

/**
 * 通用常量
 */
public class CommonConstants {

    public static final String PHONE_REGEX = "^1[3-9]\\d{9}$";
    public static final String EMAIL_REGEX = "^(?=.{1,64}@)[\\p{L}0-9_+&*-]+(?:\\.[\\p{L}0-9_+&*-]+)*@" +
            "(?:[\\p{L}0-9-]+\\.)+[\\p{L}]{2,}$";
    public static final Pattern PHONE_PATTERN = Pattern.compile(PHONE_REGEX);
    public static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

    /** RabbitMQ 验证码交换机、队列、路由键 */
    public static final String EXCHANGE_NAME = "szxy.direct";
    public static final String EMAIL_QUEUE_NAME = "direct.email";
    public static final String EMAIL_ROUTING_KEY = "email.verify";
    public static final String PHONE_QUEUE_NAME = "direct.phone";
    public static final String PHONE_ROUTING_KEY = "phone.verify";

    /** 默认头像、默认用户名前缀 */
    public static final String COMMON_AVATOR = "https://shixiaoyu-funny.oss-cn-beijing.aliyuncs.com/%E6%95%B0%E6%99%BA%E4%B9%A1%E7%BA%A6%E6%B3%A8%E5%86%8C%E5%A4%B4%E5%83%8F%E8%AE%BE%E8%AE%A1.png";
    public static final String COMMON_USERNAME_PREFIX = "szxy用户-";

    /** OSS 访问前缀 */
    public static final String OSS_PREFIX = "https://shixiaoyu-funny.oss-cn-beijing.aliyuncs.com/";
    /** RabbitMQ AI聊天异步存储交换机、队列、路由键 */
    public static final String CHAT_QUEUE_NAME = "direct.chat";
    public static final String CHAT_ROUTING_KEY = "chat.save";

    /** 位置变更 → AI 推荐邮件 */
    public static final String POS_ALTER_QUEUE_NAME = "direct.pos.alter";
    public static final String POS_ALTER_ROUTING_KEY = "pos.alter";

    /**
     * 待支付订单超时取消：延迟队列（TTL 15 分钟）→ 死信 → 取消队列
     * 下单成功后投递 orderId；到期若仍为待支付则自动取消
     */
    public static final String ORDER_PAY_DELAY_EXCHANGE = "szxy.order.pay.delay";
    public static final String ORDER_PAY_DELAY_QUEUE = "order.pay.delay";
    public static final String ORDER_PAY_DELAY_ROUTING_KEY = "order.pay.delay";
    public static final String ORDER_PAY_CANCEL_EXCHANGE = "szxy.order.pay.cancel";
    public static final String ORDER_PAY_CANCEL_QUEUE = "order.pay.cancel";
    public static final String ORDER_PAY_CANCEL_ROUTING_KEY = "order.pay.cancel";
    /** 支付超时毫秒数：15 分钟 */
    public static final int ORDER_PAY_TIMEOUT_MS = 15 * 60 * 1000;
}
