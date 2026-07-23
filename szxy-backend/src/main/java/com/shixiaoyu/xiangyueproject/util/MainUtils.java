package com.shixiaoyu.xiangyueproject.util;

import org.springframework.mail.SimpleMailMessage;

public class MainUtils {
    public static SimpleMailMessage createMessage(String title, String content, String email,String username){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setSubject(title);
        message.setText(content);
        message.setTo(email);
        message.setFrom(username);
        return message;
    }
}
