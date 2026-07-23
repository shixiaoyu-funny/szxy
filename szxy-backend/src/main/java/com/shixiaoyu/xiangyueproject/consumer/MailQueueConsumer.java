package com.shixiaoyu.xiangyueproject.consumer;

import com.shixiaoyu.xiangyueproject.constants.CommonConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.shixiaoyu.xiangyueproject.util.MainUtils.createMessage;

@Slf4j
@Component
@RabbitListener(bindings = @QueueBinding(value=@Queue(name= CommonConstants.EMAIL_QUEUE_NAME),
        exchange = @Exchange(name = CommonConstants.VERIFY_EXCHANGE_NAME),
        key = CommonConstants.EMAIL_ROUTING_KEY))
public class MailQueueConsumer {

    @Resource
    JavaMailSender sender;

    @Value("${spring.mail.username}")
    String username;

    @RabbitHandler
    public void sendMailMessage(Map<String, String> data) {
        String email = data.get("email");
        String code = data.get("code");
        String type = data.get("type");
        log.info("发送邮件：{}", email);
        SimpleMailMessage message = switch (type) {
            case "register" -> createMessage("欢迎注册我们的网站",
                    "您的邮件注册验证码为：" + code + "，有效时间3分钟，为了保障您的安全，请勿向他人泄露验证码信息。", email, username);
            case "login" -> createMessage("欢迎登录我们的网站",
                    "您的邮件登录验证码为：" + code + "，有效时间3分钟，为了保障您的安全，请勿向他人泄露验证码信息。", email, username);
            case "reset" -> createMessage("您的密码重置邮件",
                    "您好，您正在进行重置密码操作，验证码" + code + "，有效时间3分钟，如非本人操作，请无视。", email, username);
            default -> null;
        };
        if (message == null) return;
        sender.send(message);
    }
}
