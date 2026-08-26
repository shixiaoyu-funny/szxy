package com.shixiaoyu.xiangyueproject.interceptor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shixiaoyu.xiangyueproject.constants.RedisConstants;
import com.shixiaoyu.xiangyueproject.entity.dto.UserDTO;
import com.shixiaoyu.xiangyueproject.entity.po.User;
import com.shixiaoyu.xiangyueproject.mapper.UserMapper;
import com.shixiaoyu.xiangyueproject.utils.UserHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

/**
 * 登录拦截器（Redis + UUID 无状态）
 * 校验 access_token -> Redis 会话 -> 从 DB 刷新最新角色/状态（禁用即拒、任命即时生效）-> 续期 -> 存入 ThreadLocal
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LoginInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;
    private final UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("access_token");
        if (StrUtil.isBlank(token)) {
            response.setStatus(401);
            return false;
        }
        String key = RedisConstants.LOGIN_TOKEN_PREFIX + token;
        String userJson = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isBlank(userJson)) {
            response.setStatus(401);
            return false;
        }
        UserDTO cachedUser = JSONUtil.toBean(userJson, UserDTO.class);
        if (cachedUser == null || cachedUser.getId() == null) {
            response.setStatus(401);
            return false;
        }
        // 从数据库拉取最新用户：校验存在性/状态，并同步最新角色（村长任命/降级后即时生效）
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getId, cachedUser.getId()));
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            log.warn("用户 {} 不存在或已被禁用，拒绝访问", cachedUser.getId());
            response.setStatus(401);
            return false;
        }
        UserDTO freshUser = new UserDTO();
        freshUser.setId(user.getId());
        freshUser.setUsername(user.getUsername());
        freshUser.setPhone(user.getPhone());
        freshUser.setEmail(user.getEmail());
        freshUser.setRole(user.getRole());
        freshUser.setStatus(user.getStatus());

        UserHolder.saveUser(freshUser);
        // 刷新 token 有效期并回写最新用户信息（含角色变更）
        stringRedisTemplate.expire(key, RedisConstants.TOKEN_EXPIRE_TIME, TimeUnit.MINUTES);
        try {
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(freshUser),
                    RedisConstants.TOKEN_EXPIRE_TIME, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("回写用户会话失败", e);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserHolder.removeUser();
    }
}
