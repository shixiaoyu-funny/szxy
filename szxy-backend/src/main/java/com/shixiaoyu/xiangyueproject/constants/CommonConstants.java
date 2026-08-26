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
}
