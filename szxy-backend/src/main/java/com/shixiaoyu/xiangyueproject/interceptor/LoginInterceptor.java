package com.shixiaoyu.xiangyueproject.interceptor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.shixiaoyu.xiangyueproject.constants.CommonConstants;
import com.shixiaoyu.xiangyueproject.constants.RedisConstants;
import com.shixiaoyu.xiangyueproject.entity.dto.UserDTO;
import com.shixiaoyu.xiangyueproject.entity.po.User;
import com.shixiaoyu.xiangyueproject.mapper.UserMapper;
import com.shixiaoyu.xiangyueproject.util.UserHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

import static com.shixiaoyu.xiangyueproject.constants.RedisConstants.AI_MEMORY;
import static com.shixiaoyu.xiangyueproject.constants.RedisConstants.BLACK_USER;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoginInterceptor implements HandlerInterceptor {
    private final StringRedisTemplate stringRedisTemplate;
    private final UserMapper userMapper;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("authorization");
        log.info("token:{}", token);
//        2.判空
        if (StrUtil.isBlank(token)) {
            log.info("用户:{} 登录失败，用户不存在", token);
            response.setStatus(401);
            return false;
        }

//        3.基于token获取用户
        String key= RedisConstants.LOGIN_TOKEN_PREFIX+token;
        String userJson = stringRedisTemplate.opsForValue().get(key);
        if(userJson== null){
            log.info("用户:{} 登录失败，用户不存在", token);
            response.setStatus(401);
            return false;
        }
        UserDTO userDTO = JSONUtil.toBean(userJson, UserDTO.class);

        //判断用户是否在黑名单内
        String blackKey=BLACK_USER+userDTO.getId();
        String str = stringRedisTemplate.opsForValue().get(blackKey);
        if("black".equals(str)){
            log.info("用户:{} 登录失败，用户在黑名单内", token);
            response.setStatus(401);
            return false;
        }
        else{
            User user = userMapper.selectById(userDTO.getId());
            Integer status = user.getStatus();
            if(status==0){
                user.setStatus(1);
                userMapper.updateById(user);
            }
        }
        log.info("用户登录成功，准备存入 Redis 的用户信息: {}", userDTO);
//        4.判空
        if (userDTO==null) {
            log.info("用户:{} 登录失败，用户不存在", token);
            response.setStatus(401);
            return false;
        }
        //5.将查询到的用户保存到ThreadLocal
        UserHolder.saveUser(userDTO);
        //6.刷新token有效期
        stringRedisTemplate.expire(key, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        // 拿到用户 ID 后存入 request
        request.setAttribute(CommonConstants.ATTR_USER_ID, userDTO.getId());
        //放行
        return true;
    }
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}