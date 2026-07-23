package com.shixiaoyu.xiangyueproject.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shixiaoyu.xiangyueproject.constants.CommonConstants;
import com.shixiaoyu.xiangyueproject.entity.dto.UserSetInfoDTO;
import com.shixiaoyu.xiangyueproject.entity.po.User;
import com.shixiaoyu.xiangyueproject.entity.result.Result;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public interface LoginService extends IService<User> {
    Result<String> sendCode(String content,String ip);

    Result<String> phoneLogin(@NotBlank(message = "手机号不能为空") @Pattern(regexp = CommonConstants.PHONE_REGEX,message = "手机号格式错误") String phone, @NotBlank(message = "验证码不能为空") @Pattern(regexp = "^\\d{6}$",message = "验证码格式错误") String code);

    Result<String> emailLogin(@NotBlank(message = "邮箱不能为空") @Email(message = "邮箱格式错误") String email, @NotBlank(message = "验证码不能为空") @Pattern(regexp = "^\\d{6}$",message = "验证码格式错误") String code);

    Result<String> pwLogin(@NotBlank(message = "用户名不能为空") String username, @NotBlank(message = "密码不能为空") String password);

    Result<Void> infoSet(UserSetInfoDTO userSetInfoDTO);
}
