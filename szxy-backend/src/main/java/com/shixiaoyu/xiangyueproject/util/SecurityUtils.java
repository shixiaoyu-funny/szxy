package com.shixiaoyu.xiangyueproject.util;

import com.shixiaoyu.xiangyueproject.entity.dto.UserDTO;
import com.shixiaoyu.xiangyueproject.enums.RoleEnum;
import com.shixiaoyu.xiangyueproject.exception.BusinessException;

/**
 * 当前登录用户工具：所有权限判断的唯一出口
 * 禁止在业务代码中直接写 role 魔法数比较
 */
public class SecurityUtils {

    private SecurityUtils() {
    }

    public static UserDTO currentUser() {
        return UserHolder.getUser();
    }

    public static Long currentUserId() {
        return UserHolder.getUser().getId();
    }

    public static boolean isVisitor() {
        return isRole(RoleEnum.VISITOR);
    }

    public static boolean isFarmer() {
        return isRole(RoleEnum.FARMER);
    }

    public static boolean isChief() {
        return isRole(RoleEnum.CHIEF);
    }

    public static boolean isAdmin() {
        return isRole(RoleEnum.ADMIN);
    }

    /** 强制要求管理员权限，否则抛 403（管理端 Service 方法统一入口） */
    public static void requireAdmin() {
        if (!isAdmin()) {
            throw new BusinessException(403, "权限不足：仅管理员可访问");
        }
    }

    /** 是否为农户及以上（农户/村长） */
    public static boolean isFarmerOrAbove() {
        UserDTO user = currentUser();
        if (user == null || user.getRole() == null) {
            return false;
        }
        int code = user.getRole().getCode();
        return code >= RoleEnum.FARMER.getCode();
    }

    /** 是否为村长及以上（村长/管理员） */
    public static boolean isChiefOrAbove() {
        UserDTO user = currentUser();
        if (user == null || user.getRole() == null) {
            return false;
        }
        int code = user.getRole().getCode();
        return code >= RoleEnum.CHIEF.getCode();
    }

    private static boolean isRole(RoleEnum role) {
        UserDTO user = currentUser();
        return user != null && role.equals(user.getRole());
    }
}
