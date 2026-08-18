package com.shixiaoyu.xiangyueproject.consumer;

import com.shixiaoyu.xiangyueproject.constants.CommonConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 邮件验证码消费者
 */
@Slf4j
@Component
@RabbitListener(bindings = @QueueBinding(value = @Queue(name = CommonConstants.EMAIL_QUEUE_NAME),
        exchange = @Exchange(name = CommonConstants.VERIFY_EXCHANGE_NAME),
        key = CommonConstants.EMAIL_ROUTING_KEY))
public class MailQueueConsumer {

    @Resource
    private JavaMailSender sender;

    @Value("${spring.mail.username}")
    private String username;

    @RabbitHandler
    public void sendMailMessage(Map<String, String> data) {
        String email = data.get("email");
        String code = data.get("code");
        String type = data.get("type");
        log.info("发送邮件：{}", email);
        SimpleMailMessage message = switch (type) {
            case "register" -> createMessage("欢迎注册数智乡约",
                    "您的注册验证码为：" + code + "，有效时间3分钟，为了保障您的安全，请勿向他人泄露验证码信息。", email);
            case "login" -> createMessage("欢迎登录数智乡约",
                    "您的登录验证码为：" + code + "，有效时间3分钟，为了保障您的安全，请勿向他人泄露验证码信息。", email);
            case "reset" -> createMessage("密码重置",
                    "您好，您正在进行重置密码操作，验证码" + code + "，有效时间3分钟，如非本人操作，请无视。", email);
            default -> null;
        };
        if (message == null) {
            return;
        }
        sender.send(message);
    }

    private SimpleMailMessage createMessage(String title, String content, String email) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setSubject(title);
        message.setText(content);
        message.setTo(email);
        message.setFrom(username);
        return message;
    }
}
