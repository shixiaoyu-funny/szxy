package com.shixiaoyu.xiangyueproject.interceptor;

import com.shixiaoyu.xiangyueproject.entity.dto.UserDTO;
import com.shixiaoyu.xiangyueproject.enums.RoleEnum;
import com.shixiaoyu.xiangyueproject.util.UserHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 管理端拦截器：校验当前登录用户是否为管理员（role=4）
 * 仅注册到 admin-path（如 /report/**）；其余管理端方法由 Service 层 SecurityUtils.requireAdmin() 兜底
 */
@Component
@Slf4j
public class AdminInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        UserDTO user = UserHolder.getUser();
        if (user == null || user.getRole() != RoleEnum.ADMIN) {
            log.warn("越权访问管理端：uri={}, userId={}", request.getRequestURI(),
                    user == null ? null : user.getId());
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"data\":null,\"message\":\"权限不足：仅管理员可访问\"}");
            return false;
        }
        return true;
    }
}
