package com.shixiaoyu.xiangyueproject.constants;

public class RedisConstants {
    public static final String VERIFY_EMAIL_LIMIT_PREFIX = "verify:email:limit:";
    public static final String VERIFY_PHONE_LIMIT_PREFIX = "verify:phone:limit:";
    public static final String VERIFY_EMAIL_CODE_PREFIX = "verify:email:data:";
    public static final String VERIFY_PHONE_CODE_PREFIX = "verify:phone:data:";
    public static final int VERIFY_CODE_LIMIT=3;

    public static final String LOGIN_TOKEN_PREFIX="login:token:";
    public static final int TOKEN_EXPIRE_TIME=30;
    public static final int LOGIN_USER_TTL=30;

    public static final String AI_RECOMMEND_PREFIX="ai:rd:";
    public static final int AI_RECOMMEND_TTL=24;

    public static final String AI_MULTI_PREFIX="ai:multi:";
    public static final int AI_MULTI_TTL=72;

    public static final String VILLAGE_LIKES_TOP_10="village:likes:top:10";
    public static final String VILLAGE_COLLECTIONS_TOP_10="village:collections:top:10";
    public static final int VILLAGE_TOP_10_TTL=3;

    public static final String AI_MEMORY="ai:memory";
    public static final int AI_MEMORY_TTL=3;

    public static final String USER_COMMENTS="user:comments:";
    public static final String USER_LIKES="user:likes:";
    public static final String USER_COLLECTIONS="user:collections:";
    public static final int USER_COMMENTS_TTL=600;

    public static final int MAX_RETRY_COUNT=10;
    public static final int LOCK_TTL=3;
    public static final int RETRY_TIME=200;
    public static final String LOCK_LIKE_PREFIX="lock:like:";
    public static final String LOCK_COLLECT_PREFIX="lock:collect:";
    public static final String LOCK_COMMENT_PREFIX="lock:comment:";

    public static final String SCENIC_COMMENTS="scenic:comments:";

    public static final String BLACK_USER="black:user:";

}
