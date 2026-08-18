package com.shixiaoyu.xiangyueproject.entity.dto;

import com.shixiaoyu.xiangyueproject.enums.RoleEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录态用户 DTO（存入 Redis token，ThreadLocal 承载）
 * role 用强类型枚举，与 user 表一致
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
    private Long id;
    private String username;
    private String phone;
    private String email;
    private RoleEnum role;
    private Integer status;
}
