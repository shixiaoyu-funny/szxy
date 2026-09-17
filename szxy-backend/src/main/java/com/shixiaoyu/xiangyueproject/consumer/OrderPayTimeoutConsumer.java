package com.shixiaoyu.xiangyueproject.consumer;

import com.shixiaoyu.xiangyueproject.constants.CommonConstants;
import com.shixiaoyu.xiangyueproject.service.TradeOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 支付超时死信消费者
 * <p>
 * 消息路径：下单投递 delay 队列 → 趴 15 分钟 → 死信进入本队列。
 * 业务：调用 {@link TradeOrderService#cancelUnpaidTimeout}，
 * 仅当订单仍为「待支付」才取消；已支付/已取消则 CAS 失败并忽略（幂等）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RabbitListener(queues = CommonConstants.ORDER_PAY_CANCEL_QUEUE)
public class OrderPayTimeoutConsumer {

    private final TradeOrderService tradeOrderService;

    /**
     * 消息体约定：{"orderId":"123"}（与下单投递格式一致）
     * 解析失败直接丢弃；业务异常抛出以便 MQ 重试/进死信策略（视全局配置）
     */
    @RabbitHandler
    public void onTimeout(Map<String, String> msg) {
        if (msg == null || msg.get("orderId") == null) {
            log.warn("支付超时消息缺少 orderId: {}", msg);
            return;
        }
        Long orderId;
        try {
            orderId = Long.valueOf(msg.get("orderId"));
        } catch (NumberFormatException e) {
            log.warn("支付超时消息 orderId 非法: {}", msg.get("orderId"));
            return;
        }
        try {
            tradeOrderService.cancelUnpaidTimeout(orderId);
        } catch (Exception e) {
            log.error("支付超时取消失败 orderId={}", orderId, e);
            throw e;
        }
    }
}
