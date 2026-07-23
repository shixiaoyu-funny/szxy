package com.shixiaoyu.xiangyueproject.interceptor;

import com.shixiaoyu.xiangyueproject.constants.RedisConstants;
import com.shixiaoyu.xiangyueproject.util.PVUVUtils;
import com.shixiaoyu.xiangyueproject.util.UserHolder;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class FreshInterceptor implements HandlerInterceptor {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private PVUVUtils pvuvUtils;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token=request.getHeader("authorization");
        //设置key
        String key= RedisConstants.LOGIN_TOKEN_PREFIX+token;
        //刷新token有效期
        stringRedisTemplate.expire(key, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        //插入pv/uv
        Integer type = UserHolder.getUser().getType();
        if(type!=3){
            pvuvUtils.addUV(LocalDate.now(),token);
            pvuvUtils.incrPV(LocalDate.now());
        }
        return true;
    }
}
