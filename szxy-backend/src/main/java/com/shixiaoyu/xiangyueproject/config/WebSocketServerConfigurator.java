package com.shixiaoyu.xiangyueproject.config;

import jakarta.websocket.server.ServerEndpointConfig;

/**
 * 允许跨域 Origin 的 WebSocket 握手（开发环境管理端通常为 5173，后端为 8080）。
 * Tomcat 在某些版本/配置下会对 Origin 做校验，导致 @OnOpen 不触发但业务端仍可调用群发（连接数为 0）。
 */
public class WebSocketServerConfigurator extends ServerEndpointConfig.Configurator {
    @Override
    public boolean checkOrigin(String originHeaderValue) {
        return true;
    }
}

