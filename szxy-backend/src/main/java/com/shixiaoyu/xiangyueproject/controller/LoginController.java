package com.shixiaoyu.xiangyueproject.controller;

import com.shixiaoyu.xiangyueproject.constants.CommonConstants;
import com.shixiaoyu.xiangyueproject.entity.dto.UserSetInfoDTO;
import com.shixiaoyu.xiangyueproject.entity.po.User;
import com.shixiaoyu.xiangyueproject.entity.result.Result;
import com.shixiaoyu.xiangyueproject.service.LoginService;
import com.shixiaoyu.xiangyueproject.util.ClientUtils;
import com.shixiaoyu.xiangyueproject.util.UserHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.shixiaoyu.xiangyueproject.constants.RedisConstants.AI_MEMORY;

@Slf4j
@RestController
@RequestMapping("/common/login")
@Tag(name = "登录接口（通用）")
@Validated
@RequiredArgsConstructor
public class LoginController {
    private final LoginService loginService;
    private final StringRedisTemplate stringRedisTemplate;
    /**
     * 发送验证码
     * @param content
     * @return
     */
    @Operation(summary = "发送验证码")
    @PostMapping("/sendcode")
    public Result<String> sendCode(@Parameter(description = "手机号/邮箱",name = "content",required = true,in = ParameterIn.QUERY) @NotBlank(message = "手机号/邮箱不能为空") @RequestParam String content,
                                   HttpServletRequest request) {
        return loginService.sendCode(content, ClientUtils.getClientIp(request));
    }

    /**
     * 手机号登录
     * @param phone
     * @param code
     * @return
     */
    @Operation(summary = "手机号登录/注册",
    parameters = {
            @Parameter(description = "手机号",name = "phone",required = true,in = ParameterIn.QUERY),
            @Parameter(description = "验证码",name = "code",required = true,in = ParameterIn.QUERY)
    })
    @PostMapping("/phone_login")
    public Result<String> phoneLogin(@RequestParam @NotBlank(message = "手机号不能为空") @Pattern(regexp = CommonConstants.PHONE_REGEX,message = "手机号格式错误") String phone,
                                     @NotBlank(message = "验证码不能为空") @Pattern(regexp = "^\\d{6}$",message = "验证码格式错误")@RequestParam String code){
        return loginService.phoneLogin(phone,code);
    }

    /**
     * 邮箱登录
     * @param email
     * @param code
     * @return
     */
    @Operation(summary = "邮箱登录/注册",
    parameters = {
            @Parameter(description = "邮箱",name = "email",required = true,in = ParameterIn.QUERY),
            @Parameter(description = "验证码",name = "code",required = true,in = ParameterIn.QUERY)
    })
    @PostMapping("/email_login")
    public Result<String> emailLogin(@RequestParam @NotBlank(message = "邮箱不能为空") @Email(message = "邮箱格式错误") String email,
                                     @RequestParam @NotBlank(message = "验证码不能为空") @Pattern(regexp = "^\\d{6}$",message = "验证码格式错误") String code){
        return loginService.emailLogin(email,code);
    }

    /**
     * 账密登录
     * @param username
     * @param password
     * @return
     */
    @Operation(summary = "账密登录",
    parameters =  {
            @Parameter(description = "用户名",name = "username",required = true,in = ParameterIn.QUERY),
            @Parameter(description = "密码",name = "password",required = true,in = ParameterIn.QUERY)
    })
    @PostMapping("/pw_login")
    public Result<String> pwLogin(@RequestParam @NotBlank(message = "用户名不能为空") String username,
                                 @RequestParam @NotBlank(message = "密码不能为空") String password){
        return loginService.pwLogin(username,password);
    }

    /**
     * 个人信息设置
     * @param userSetInfoDTO
     * @return
     */
    @Operation(summary = "个人信息设置")
    @PostMapping("/info_set")
    public Result<Void> infoSet(@Parameter(description = "用户信息",name = "userSetInfoDTO",required = true,in = ParameterIn.QUERY) @RequestBody UserSetInfoDTO userSetInfoDTO){
        return loginService.infoSet(userSetInfoDTO);
    }

    /**
     * 退出登录
     * @return
     */
    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout(){
        Long userId = UserHolder.getUser().getId();
        log.info("用户:{} 退出登录", userId);
        UserHolder.removeUser();
        stringRedisTemplate.delete(AI_MEMORY + userId.toString());
        return Result.ok();
    }
}
