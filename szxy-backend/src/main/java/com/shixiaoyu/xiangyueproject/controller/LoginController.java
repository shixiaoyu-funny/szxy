package com.shixiaoyu.xiangyueproject.controller;

import cn.hutool.core.util.StrUtil;
import com.shixiaoyu.xiangyueproject.constants.CommonConstants;
import com.shixiaoyu.xiangyueproject.constants.RedisConstants;
import com.shixiaoyu.xiangyueproject.entity.dto.UserSetInfoDTO;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录接口（通用）
 */
@Slf4j
@RestController
@RequestMapping("/lg")
@Tag(name = "登录接口（通用）")
@Validated
@RequiredArgsConstructor
public class LoginController {
    private final LoginService loginService;
    private final StringRedisTemplate stringRedisTemplate;

    @Operation(summary = "发送验证码")
    @PostMapping("/sdcode")
    public Result<String> sendCode(@Parameter(description = "手机号/邮箱", name = "content", required = true, in = ParameterIn.QUERY)
                                   @NotBlank(message = "手机号/邮箱不能为空") @RequestParam String content,
                                   HttpServletRequest request) {
        return Result.ok(loginService.sendCode(content, ClientUtils.getClientIp(request)));
    }

    @Operation(summary = "手机号登录/注册",
            parameters = {
                    @Parameter(description = "手机号", name = "phone", required = true, in = ParameterIn.QUERY),
                    @Parameter(description = "验证码", name = "code", required = true, in = ParameterIn.QUERY)
            })
    @PostMapping("/ph")
    public Result<String> phoneLogin(
            @RequestParam @NotBlank(message = "手机号不能为空") @Pattern(regexp = CommonConstants.PHONE_REGEX, message = "手机号格式错误") String phone,
            @RequestParam @NotBlank(message = "验证码不能为空") @Pattern(regexp = "^\\d{6}$", message = "验证码格式错误") String code) {
        return Result.ok(loginService.phoneLogin(phone, code));
    }

    @Operation(summary = "邮箱登录/注册",
            parameters = {
                    @Parameter(description = "邮箱", name = "email", required = true, in = ParameterIn.QUERY),
                    @Parameter(description = "验证码", name = "code", required = true, in = ParameterIn.QUERY)
            })
    @PostMapping("/em")
    public Result<String> emailLogin(
            @RequestParam @NotBlank(message = "邮箱不能为空") @Email(message = "邮箱格式错误") String email,
            @RequestParam @NotBlank(message = "验证码不能为空") @Pattern(regexp = "^\\d{6}$", message = "验证码格式错误") String code) {
        return Result.ok(loginService.emailLogin(email, code));
    }

    @Operation(summary = "账密登录",
            parameters = {
                    @Parameter(description = "用户名", name = "username", required = true, in = ParameterIn.QUERY),
                    @Parameter(description = "密码", name = "password", required = true, in = ParameterIn.QUERY)
            })
    @PostMapping("/pw")
    public Result<String> pwLogin(@RequestParam @NotBlank(message = "用户名不能为空") String username,
                                  @RequestParam @NotBlank(message = "密码不能为空") String password) {
        return Result.ok(loginService.pwLogin(username, password));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/lgout")
    public Result<Void> logout(HttpServletRequest request) {
        String token = request.getHeader("authorization");
        if (StrUtil.isNotBlank(token)) {
            stringRedisTemplate.delete(RedisConstants.LOGIN_TOKEN_PREFIX + token);
        }
        UserHolder.removeUser();
        return Result.ok();
    }
}
