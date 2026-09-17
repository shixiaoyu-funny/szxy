package com.shixiaoyu.xiangyueproject.common.config;

import com.shixiaoyu.xiangyueproject.constants.CommonConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 订单支付超时延迟队列（经典 TTL + 死信，无需延时插件）
 * <pre>
 * 下单成功 → 发消息到 delay 交换机/队列（消息 TTL=15min）
 *          → 到期未消费 → 死信到 cancel 交换机/队列
 *          → OrderPayTimeoutConsumer 若仍待支付则取消
 * </pre>
 */
@Configuration
public class OrderPayDelayConfiguration {

    /** 延迟消息进入的交换机 */
    @Bean
    public DirectExchange orderPayDelayExchange() {
        return new DirectExchange(CommonConstants.ORDER_PAY_DELAY_EXCHANGE, true, false);
    }

    /**
     * 延迟队列：不挂业务消费者；消息在此等待 TTL，过期后转发到死信交换机
     */
    @Bean
    public Queue orderPayDelayQueue() {
        return QueueBuilder.durable(CommonConstants.ORDER_PAY_DELAY_QUEUE)
                .ttl(CommonConstants.ORDER_PAY_TIMEOUT_MS)
                .deadLetterExchange(CommonConstants.ORDER_PAY_CANCEL_EXCHANGE)
                .deadLetterRoutingKey(CommonConstants.ORDER_PAY_CANCEL_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding orderPayDelayBinding() {
        return BindingBuilder.bind(orderPayDelayQueue())
                .to(orderPayDelayExchange())
                .with(CommonConstants.ORDER_PAY_DELAY_ROUTING_KEY);
    }

    /** 死信（真正执行取消）交换机 */
    @Bean
    public DirectExchange orderPayCancelExchange() {
        return new DirectExchange(CommonConstants.ORDER_PAY_CANCEL_EXCHANGE, true, false);
    }

    /** 取消队列：由 OrderPayTimeoutConsumer 消费 */
    @Bean
    public Queue orderPayCancelQueue() {
        return QueueBuilder.durable(CommonConstants.ORDER_PAY_CANCEL_QUEUE).build();
    }

    @Bean
    public Binding orderPayCancelBinding() {
        return BindingBuilder.bind(orderPayCancelQueue())
                .to(orderPayCancelExchange())
                .with(CommonConstants.ORDER_PAY_CANCEL_ROUTING_KEY);
    }
}
