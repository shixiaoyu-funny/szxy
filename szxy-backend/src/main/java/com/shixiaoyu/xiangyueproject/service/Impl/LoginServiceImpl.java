package com.shixiaoyu.xiangyueproject.service.Impl;

import cn.hutool.Hutool;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shixiaoyu.xiangyueproject.constants.CommonConstants;
import com.shixiaoyu.xiangyueproject.constants.ErrorConstants;
import com.shixiaoyu.xiangyueproject.constants.RedisConstants;
import com.shixiaoyu.xiangyueproject.entity.dto.UserDTO;
import com.shixiaoyu.xiangyueproject.entity.dto.UserSetInfoDTO;
import com.shixiaoyu.xiangyueproject.entity.po.User;
import com.shixiaoyu.xiangyueproject.entity.result.Result;
import com.shixiaoyu.xiangyueproject.mapper.LoginMapper;
import com.shixiaoyu.xiangyueproject.service.LoginService;
import com.shixiaoyu.xiangyueproject.util.FlowUtils;
import com.shixiaoyu.xiangyueproject.util.UserHolder;
import com.shixiaoyu.xiangyueproject.util.VerifyUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class LoginServiceImpl extends ServiceImpl<LoginMapper, User> implements LoginService {
    @Resource
    private VerifyUtils verifyUtils;

    @Resource
    private AmqpTemplate amqpTemplate;

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private LoginMapper loginMapper;
    @Resource
    private BCryptPasswordEncoder encoder;

    @Override
    public Result<String> sendCode(String content, String ip) {
        synchronized (ip.intern()) {
            String code = String.valueOf(RandomUtil.randomInt(100000, 999999));
            try {
                if (content.matches(CommonConstants.EMAIL_REGEX)) {
                    // 邮箱验证码：校验邮箱限流
                    if (!verifyUtils.verifyEmailLimit(ip)) {
                        log.warn("IP:{} 邮箱验证码发送频率超限", ip);
                        return Result.error(ErrorConstants.VERIFY_LIMIT);
                    }
                    //查看用户是否存在
                    User user = getOne(new LambdaQueryWrapper<User>().eq(User::getEmail, content));
                    String type;
                    if(user == null){
                        type = "register";
                    }
                    else{
                        type = "login";
                    }
                    // type传给MQ，用于区分重置密码/登录等操作
                    Map<String, String> data = Map.of("type", type, "email", content, "code", code);
                    amqpTemplate.convertAndSend(CommonConstants.VERIFY_EXCHANGE_NAME, CommonConstants.EMAIL_ROUTING_KEY, data);
                    // 存入Redis
                    stringRedisTemplate.opsForValue()
                            .set(RedisConstants.VERIFY_EMAIL_CODE_PREFIX + content, code, RedisConstants.VERIFY_CODE_LIMIT, TimeUnit.MINUTES);
                    log.info("邮箱:{} 验证码已发送（type:{}），IP:{}", content, type, ip);
                } else if (content.matches(CommonConstants.PHONE_REGEX)) {
                    // 手机号验证码：校验手机号限流
                    if (!verifyUtils.verifyPhoneLimit(ip)) {
                        log.warn("IP:{} 手机号验证码发送频率超限", ip);
                        return Result.error(ErrorConstants.VERIFY_LIMIT);
                    }
                    // type传给MQ，用于区分重置密码/登录等操作
                    Map<String, String> data = Map.of("phone", content, "code", code);
                    try{
                        amqpTemplate.convertAndSend(CommonConstants.VERIFY_EXCHANGE_NAME, CommonConstants.PHONE_ROUTING_KEY, data);
                        // 存入Redis
                        stringRedisTemplate.opsForValue()
                                .set(RedisConstants.VERIFY_PHONE_CODE_PREFIX + content, code, RedisConstants.VERIFY_CODE_LIMIT, TimeUnit.MINUTES);
                        log.info("手机号:{} 验证码已发送，IP:{}", content, ip);
                    } catch (AmqpException e) {
                        log.error("MQ发送异常，data:{}", JSONUtil.toJsonStr(data), e);
                        return Result.error(ErrorConstants.MQ_SEND_ERROR);
                    }

                } else {
                    log.error("内容格式错误，非手机号/邮箱：{}", content);
                    return Result.error(ErrorConstants.PHONE_OR_EMAIL_REGEX_ERROR);
                }
                return Result.ok("验证码发送成功");
            } catch (Exception e) {
                log.error("发送验证码失败，content:{},  ip:{}", content, ip, e);
                return Result.error(ErrorConstants.VERIFY_CODE_SEND_ERROR);
            }
        }
    }

    @Override
    public Result<String> phoneLogin(String phone, String code) {
        try {
            // 验证码校验
            String redisCode = stringRedisTemplate.opsForValue().get(RedisConstants.VERIFY_PHONE_CODE_PREFIX + phone);
            if (redisCode == null) {
                log.warn("手机号:{} 验证码不存在", phone);
                return Result.error(ErrorConstants.NO_VERIFY_CODE);
            }
            if (!redisCode.equals(code)) {
                log.warn("手机号:{} 验证码错误，传入:{}，正确:{}", phone, code, redisCode);
                return Result.error(ErrorConstants.VERIFY_CODE_ERROR);
            }

            // 查询用户是否存在，不存在则创建
            User user = loginMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
            if (user == null) {
                user = new User();
                user.setPhone(phone);
                user.setType(1);
                user.setStatus(1);
                user.setAvator(CommonConstants.COMMON_AVATOR);
                user.setUsername(CommonConstants.COMMON_USERNAME_PREFIX + phone);
                int insert = loginMapper.insert(user);
                if (insert != 1) {
                    log.error("创建手机用户失败，phone:{}", phone);
                    return Result.error(ErrorConstants.USER_CREATE_ERROR);
                }
                log.info("创建手机用户成功，phone:{}", phone);
            }

            // 生成token并存储
            String token = UUID.randomUUID().toString(true);
            String key = RedisConstants.LOGIN_TOKEN_PREFIX + token;
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(BeanUtil.copyProperties(user, UserDTO.class)), RedisConstants.TOKEN_EXPIRE_TIME, TimeUnit.MINUTES);
            log.info("手机号:{} 登录成功，token:{}", phone, token);
            return Result.ok(token);
        } catch (Exception e) {
            log.error("手机号登录失败，phone:{}", phone, e);
            return Result.error(ErrorConstants.LOGIN_ERROR);
        }
    }

    @Override
    public Result<String> emailLogin(String email, String code) {
        try {
            // 验证码校验
            String redisCode = stringRedisTemplate.opsForValue().get(RedisConstants.VERIFY_EMAIL_CODE_PREFIX + email);
            if (redisCode == null) {
                log.warn("邮箱:{} 验证码不存在", email);
                return Result.error(ErrorConstants.NO_VERIFY_CODE);
            }
            if (!redisCode.equals(code)) {
                log.warn("邮箱:{} 验证码错误，传入:{}，正确:{}", email, code, redisCode);
                return Result.error(ErrorConstants.VERIFY_CODE_ERROR);
            }

            // 查询用户是否存在，不存在则创建（修复空指针+字段赋值错误）
            User user = loginMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
            if (user == null) {
                user = new User();
                user.setEmail(email);
                user.setType(1);
                user.setStatus(1);
                user.setAvator(CommonConstants.COMMON_AVATOR);
                user.setUsername(CommonConstants.COMMON_USERNAME_PREFIX + email);
                int insert = loginMapper.insert(user);
                if (insert != 1) {
                    log.error("创建邮箱用户失败，email:{}", email);
                    return Result.error(ErrorConstants.USER_CREATE_ERROR);
                }
                log.info("创建邮箱用户成功，email:{}", email);
            }

            // 生成token并存储
            String token = UUID.randomUUID().toString(true);
            String key = RedisConstants.LOGIN_TOKEN_PREFIX + token;
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(BeanUtil.copyProperties(user, UserDTO.class)), RedisConstants.TOKEN_EXPIRE_TIME, TimeUnit.MINUTES);
            log.info("邮箱:{} 登录成功，token:{}", email, token);
            return Result.ok(token);
        } catch (Exception e) {
            log.error("邮箱登录失败，email:{}", email, e);
            return Result.error(ErrorConstants.LOGIN_ERROR);
        }
    }

    @Override
    public Result<String> pwLogin(String username, String password) {
        User user = loginMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if(user == null){
            log.error("用户:{} 不存在", username);
            return Result.error(ErrorConstants.USER_NOT_EXIST);
        }
        boolean res = encoder.matches(password, user.getPassword());
        if(!res){
            log.error("用户:{} 密码错误", username);
            return Result.error(ErrorConstants.PASSWORD_ERROR);

        }//生成 DTO 并显式检查 ID
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        if (userDTO.getId() == null && user.getId() != null) {
            userDTO.setId(user.getId()); // 手动赋值
        }
        String token = UUID.randomUUID().toString(true);
        String key = RedisConstants.LOGIN_TOKEN_PREFIX + token;
        // 存入 Redis
        log.info("用户:{} 登录成功，准备存入 Redis 的用户信息: {}", username, userDTO);
        stringRedisTemplate.opsForValue().set(
                key,
                JSONUtil.toJsonStr(userDTO),
                RedisConstants.TOKEN_EXPIRE_TIME,
                TimeUnit.MINUTES
        );
        log.info("用户:{} 登录成功，token:{}", username, token);
        return Result.ok(token);
    }

    @Override
    public Result<Void> infoSet(UserSetInfoDTO userSetInfoDTO) {
        String password = userSetInfoDTO.getPassword();
        if(password != null){
            String encode = encoder.encode(password);
            userSetInfoDTO.setPassword(encode);
        }
        userSetInfoDTO.setId(UserHolder.getUser().getId());
        User user = BeanUtil.copyProperties(userSetInfoDTO, User.class);
        log.info("用户:{} 设置用户信息成功，用户信息:{}", UserHolder.getUser().getId(), user);
        loginMapper.updateById(user);
        return Result.ok();
    }
}