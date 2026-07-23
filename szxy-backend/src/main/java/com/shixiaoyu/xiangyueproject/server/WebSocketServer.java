package com.shixiaoyu.xiangyueproject.server;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ServerEndpoint(value = "/ws/{sid}", configurator = com.shixiaoyu.xiangyueproject.config.WebSocketServerConfigurator.class)
@Slf4j
public class WebSocketServer {
    //存储会话对象
    private static final Map<String, Session> sessionMap = new ConcurrentHashMap<>();

    //连接成功调用
    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String sid){
        log.info("客户端{}建立连接，sessionId={}", sid, session.getId());
        sessionMap.put(sid, session);
    }
    //收到客户端消息后调用
    @OnMessage
    public void onMessage(String message, @PathParam("sid") String sid){
        log.info("收到来自客户端{}的消息：{}", sid, message);
    }
    //连接关闭
    @OnClose
    public void onClose(@PathParam("sid") String sid){
        log.info("连接断开：{}", sid);
        sessionMap.remove(sid);
    }

    @OnError
    public void onError(Session session, Throwable error, @PathParam("sid") String sid) {
        log.warn("WebSocket 异常，sid={}, sessionId={}, err={}", sid, session != null ? session.getId() : null, error.toString(), error);
    }
    //服务器->客户端群发消息
    public void sendToAllClient(String message){
        Collection<Session> sessions = sessionMap.values();
        log.info("群发消息：{}",message);
        log.info("当前连接数：{}",sessions.size());
        for(Session session:sessions){
            try {
                session.getBasicRemote().sendText(message);
                log.info("发送消息：{}",message);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
