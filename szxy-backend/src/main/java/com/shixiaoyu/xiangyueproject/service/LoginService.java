package com.shixiaoyu.xiangyueproject.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shixiaoyu.xiangyueproject.entity.dto.UserSetInfoDTO;
import com.shixiaoyu.xiangyueproject.entity.po.User;

/**
 * 登录/注册服务（错误时抛 BusinessException，由全局异常处理器统一返回）
 */
public interface LoginService extends IService<User> {

    /** 发送验证码（手机/邮箱），返回提示 */
    String sendCode(String content, String ip);

    /** 手机验证码登录/注册，返回 token */
    String phoneLogin(String phone, String code);

    /** 邮箱验证码登录/注册，返回 token */
    String emailLogin(String email, String code);

    /** 账密登录，返回 token */
    String pwLogin(String username, String password);

    /** 个人信息设置（登录后） */
    void infoSet(UserSetInfoDTO userSetInfoDTO);
}
