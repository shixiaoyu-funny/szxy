package com.shixiaoyu.xiangyueproject.constants;

/**
 * Redis Key 常量
 */
public class RedisConstants {

    /**
     * 验证码限流与存储
     */
    public static final String VERIFY_EMAIL_LIMIT_PREFIX = "verify:email:limit:";
    public static final String VERIFY_PHONE_LIMIT_PREFIX = "verify:phone:limit:";
    public static final String VERIFY_EMAIL_CODE_PREFIX = "verify:email:data:";
    public static final String VERIFY_PHONE_CODE_PREFIX = "verify:phone:data:";
    public static final int VERIFY_CODE_LIMIT = 3;

    /**
     * 登录 token
     */
    public static final String LOGIN_TOKEN_PREFIX = "login:token:";
    public static final int TOKEN_EXPIRE_TIME = 360; //6小时过期

    /**
     * 用户黑名单
     */
    public static final String BLACK_USER = "black:user:";

    /**
     * 用户点赞/收藏/评论历史缓存
     */
    public static final String USER_COMMENTS = "user:comments:";
    public static final String USER_LIKES = "user:likes:";
    public static final String USER_COLLECTIONS = "user:collections:";
    public static final int USER_COMMENTS_TTL = 600;

    /**
     * 景点评论缓存
     */
    public static final String SCENIC_COMMENTS = "scenic:comments:";

    /**
     * 农村 top10 排行缓存
     */
    public static final String VILLAGE_LIKES_TOP_10 = "village:likes:top:10";
    public static final String VILLAGE_COLLECTIONS_TOP_10 = "village:collections:top:10";
    public static final int VILLAGE_TOP_10_TTL = 3;

    /**
     * 缓存重建锁
     */
    public static final int MAX_RETRY_COUNT = 10;
    public static final int LOCK_TTL = 3;
    public static final int RETRY_TIME = 200;
    public static final String LOCK_LIKE_PREFIX = "lock:like:";
    public static final String LOCK_COLLECT_PREFIX = "lock:collect:";
    public static final String LOCK_COMMENT_PREFIX = "lock:comment:";

    /**
     * 省市区经纬度缓存前缀。
     * 完整 key 形如：pos:河北省:石家庄市:长安区
     * value 为 String：经度,纬度（与高德 geocode location 一致，无 TTL）
     */
    public static final String POS_PREFIX = "pos:";

    /**
     * 用户上一次上报的 IP 定位（省|市|区）
     * 完整 key：user:last:pos:{userId}
     */
    public static final String USER_LAST_POS_PREFIX = "user:last:pos:";

    /**
     * ai会话相关缓存键
     */
    public static final String AI_CHAT_SESSION_PREFIX = "chat:session:";
    public static final int AI_CHAT_SESSION_TTL = 24; //24小时过期
}
