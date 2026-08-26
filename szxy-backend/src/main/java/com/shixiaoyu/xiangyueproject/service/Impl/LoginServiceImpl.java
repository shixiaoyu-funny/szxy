package com.shixiaoyu.xiangyueproject.service.Impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixiaoyu.xiangyueproject.constants.CommonConstants;
import com.shixiaoyu.xiangyueproject.constants.ErrorConstants;
import com.shixiaoyu.xiangyueproject.constants.RedisConstants;
import com.shixiaoyu.xiangyueproject.entity.dto.UserSetInfoDTO;
import com.shixiaoyu.xiangyueproject.entity.po.User;
import com.shixiaoyu.xiangyueproject.enums.RoleEnum;
import com.shixiaoyu.xiangyueproject.exception.BusinessException;
import com.shixiaoyu.xiangyueproject.mapper.UserMapper;
import com.shixiaoyu.xiangyueproject.service.LoginService;
import com.shixiaoyu.xiangyueproject.utils.TokenUtil;
import com.shixiaoyu.xiangyueproject.utils.UserHolder;
import com.shixiaoyu.xiangyueproject.utils.VerifyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 登录/注册实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LoginServiceImpl extends ServiceImpl<UserMapper, User> implements LoginService {

    private final VerifyUtil verifyUtil;
    private final AmqpTemplate amqpTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;
    private final TokenUtil tokenUtil;

    @Override
    public String sendCode(String content, String ip) {
        synchronized (ip.intern()) {
            String code = String.valueOf(RandomUtil.randomInt(100000, 999999));
            try {
                if (CommonConstants.EMAIL_PATTERN.matcher(content).matches()) {
                    if (!verifyUtil.verifyEmailLimit(ip)) {
                        log.warn("IP:{} 邮箱验证码发送频率超限", ip);
                        throw new BusinessException(ErrorConstants.VERIFY_LIMIT);
                    }
                    User user = getOne(new LambdaQueryWrapper<User>().eq(User::getEmail, content));
                    String type = user == null ? "register" : "login";
                    Map<String, String> data = Map.of("type", type, "email", content, "code", code);
                    amqpTemplate.convertAndSend(CommonConstants.EXCHANGE_NAME, CommonConstants.EMAIL_ROUTING_KEY, data);
                    stringRedisTemplate.opsForValue().set(
                            RedisConstants.VERIFY_EMAIL_CODE_PREFIX + content, code, RedisConstants.VERIFY_CODE_LIMIT, TimeUnit.MINUTES);
                    log.info("邮箱:{} 验证码已发送（type:{}），IP:{}", content, type, ip);
                } else if (CommonConstants.PHONE_PATTERN.matcher(content).matches()) {
                    if (!verifyUtil.verifyPhoneLimit(ip)) {
                        log.warn("IP:{} 手机号验证码发送频率超限", ip);
                        throw new BusinessException(ErrorConstants.VERIFY_LIMIT);
                    }
                    Map<String, String> data = Map.of("phone", content, "code", code);
                    try {
                        amqpTemplate.convertAndSend(CommonConstants.EXCHANGE_NAME, CommonConstants.PHONE_ROUTING_KEY, data);
                        stringRedisTemplate.opsForValue().set(
                                RedisConstants.VERIFY_PHONE_CODE_PREFIX + content, code, RedisConstants.VERIFY_CODE_LIMIT, TimeUnit.MINUTES);
                        log.info("手机号:{} 验证码已发送，IP:{}", content, ip);
                    } catch (AmqpException e) {
                        log.error("MQ发送异常，phone:{}", content, e);
                        throw new BusinessException(ErrorConstants.MQ_SEND_ERROR);
                    }
                } else {
                    log.error("内容格式错误，非手机号/邮箱：{}", content);
                    throw new BusinessException(ErrorConstants.PHONE_OR_EMAIL_REGEX_ERROR);
                }
                return "验证码发送成功";
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("发送验证码失败，content:{}, ip:{}", content, ip, e);
                throw new BusinessException(ErrorConstants.VERIFY_CODE_SEND_ERROR);
            }
        }
    }

    @Override
    public String phoneLogin(String phone, String code) {
        try {
            String redisCode = stringRedisTemplate.opsForValue().get(RedisConstants.VERIFY_PHONE_CODE_PREFIX + phone);
            if (redisCode == null) {
                throw new BusinessException(ErrorConstants.NO_VERIFY_CODE);
            }
            if (!redisCode.equals(code)) {
                throw new BusinessException(ErrorConstants.VERIFY_CODE_ERROR);
            }
            User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
            if (user == null) {
                user = newUser();
                user.setPhone(phone);
                user.setUsername(CommonConstants.COMMON_USERNAME_PREFIX + phone);
                userMapper.insert(user);
                log.info("创建手机用户成功，phone:{}", phone);
            }
            return tokenUtil.issue(user);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("手机号登录失败，phone:{}", phone, e);
            throw new BusinessException(ErrorConstants.LOGIN_ERROR);
        }
    }

    @Override
    public String emailLogin(String email, String code) {
        try {
            String redisCode = stringRedisTemplate.opsForValue().get(RedisConstants.VERIFY_EMAIL_CODE_PREFIX + email);
            if (redisCode == null) {
                throw new BusinessException(ErrorConstants.NO_VERIFY_CODE);
            }
            if (!redisCode.equals(code)) {
                throw new BusinessException(ErrorConstants.VERIFY_CODE_ERROR);
            }
            User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
            if (user == null) {
                user = newUser();
                user.setEmail(email);
                user.setUsername(CommonConstants.COMMON_USERNAME_PREFIX + email);
                userMapper.insert(user);
                log.info("创建邮箱用户成功，email:{}", email);
            }
            return tokenUtil.issue(user);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("邮箱登录失败，email:{}", email, e);
            throw new BusinessException(ErrorConstants.LOGIN_ERROR);
        }
    }

    @Override
    public String pwLogin(String username, String password) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            log.warn("用户:{} 不存在", username);
            throw new BusinessException(ErrorConstants.USER_NOT_EXIST);
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            log.warn("用户:{} 已被禁用", username);
            throw new BusinessException("账号已被禁用");
        }
        if (!BCrypt.checkpw(password, user.getPassword())) {
            log.warn("用户:{} 密码错误", username);
            throw new BusinessException(ErrorConstants.PASSWORD_ERROR);
        }
        return tokenUtil.issue(user);
    }

    @Override
    public void infoSet(UserSetInfoDTO userSetInfoDTO) {
        if (userSetInfoDTO.getPassword() != null && !userSetInfoDTO.getPassword().isBlank()) {
            userSetInfoDTO.setPassword(BCrypt.hashpw(userSetInfoDTO.getPassword()));
        } else {
            userSetInfoDTO.setPassword(null);
        }
        User user = new User();
        user.setId(UserHolder.getUser().getId());
        user.setUsername(userSetInfoDTO.getUsername());
        user.setPassword(userSetInfoDTO.getPassword());
        user.setPhone(userSetInfoDTO.getPhone());
        user.setEmail(userSetInfoDTO.getEmail());
        user.setAvatar(userSetInfoDTO.getAvatar());
        userMapper.updateById(user);
    }

    /** 新建游客用户（默认角色、默认头像、默认状态） */
    private User newUser() {
        User user = new User();
        user.setRole(RoleEnum.VISITOR);
        user.setStatus(1);
        user.setAvatar(CommonConstants.COMMON_AVATOR);
        return user;
    }
}
