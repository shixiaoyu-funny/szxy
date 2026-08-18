package com.shixiaoyu.xiangyueproject.interceptor;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixiaoyu.xiangyueproject.common.properties.AuthProperties;
import com.shixiaoyu.xiangyueproject.constants.CommonConstants;
import com.shixiaoyu.xiangyueproject.constants.RedisConstants;
import com.shixiaoyu.xiangyueproject.entity.dto.UserDTO;
import com.shixiaoyu.xiangyueproject.entity.po.User;
import com.shixiaoyu.xiangyueproject.entity.result.Result;
import com.shixiaoyu.xiangyueproject.enums.RoleEnum;
import com.shixiaoyu.xiangyueproject.mapper.UserMapper;
import com.shixiaoyu.xiangyueproject.util.UserHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

/**
 * 登录拦截器
 * 校验 token -> 黑名单 -> 校验用户状态（禁用直接拒绝，不再自动解封）-> 刷新会话（含角色）-> 存入 ThreadLocal
 * 统一承担用户端与管理端的登录校验：命中 auth.include-path（如 /admin/**、/report/**）时强制 role=4（管理员）
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LoginInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;
    private final AuthProperties authProperties;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("authorization");
        if (StrUtil.isBlank(token)) {
            response.setStatus(401);
            return false;
        }
        String key = RedisConstants.LOGIN_TOKEN_PREFIX + token;
        String userJson = stringRedisTemplate.opsForValue().get(key);
        if (userJson == null) {
            response.setStatus(401);
            return false;
        }
        UserDTO cachedUser;
        try {
            cachedUser = objectMapper.readValue(userJson, UserDTO.class);
        } catch (Exception e) {
            response.setStatus(401);
            return false;
        }
        // 黑名单检查
        String blackKey = RedisConstants.BLACK_USER + cachedUser.getId();
        if ("black".equals(stringRedisTemplate.opsForValue().get(blackKey))) {
            response.setStatus(401);
            return false;
        }
        // 从数据库拉取最新用户：校验存在性/状态，并同步最新角色（任命村长/降级后即时生效）
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

        // 管理端角色校验：命中 include-path 时要求 role=4（管理员）
        if (matchesAdminPath(request.getRequestURI())) {
            if (freshUser.getRole() != RoleEnum.ADMIN) {
                log.warn("越权访问管理端：uri={}, userId={}, role={}",
                        request.getRequestURI(), freshUser.getId(), freshUser.getRole());
                response.setStatus(403);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(objectMapper.writeValueAsString(Result.error(403, "权限不足：仅管理员可访问")));
                return false;
            }
        }

        UserHolder.saveUser(freshUser);
        // 刷新 token 有效期并回写最新用户信息（含角色变更）
        stringRedisTemplate.expire(key, RedisConstants.TOKEN_EXPIRE_TIME, TimeUnit.MINUTES);
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(freshUser),
                    RedisConstants.TOKEN_EXPIRE_TIME, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("回写用户会话失败", e);
        }
        request.setAttribute(CommonConstants.ATTR_USER_ID, user.getId());
        return true;
    }

    /**
     * 判断请求路径是否命中管理端 include-path（如 /admin/**、/report/**）
     */
    private boolean matchesAdminPath(String uri) {
        if (StrUtil.isBlank(uri) || authProperties.getIncludePath() == null) {
            return false;
        }
        for (String pattern : authProperties.getIncludePath()) {
            if (pathMatcher.match(pattern, uri)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserHolder.removeUser();
    }
}
