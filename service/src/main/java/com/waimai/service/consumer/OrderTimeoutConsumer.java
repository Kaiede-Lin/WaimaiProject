package com.waimai.service.consumer;

import com.rabbitmq.client.Channel;
import com.waimai.service.config.RabbitMQConfig;
import com.waimai.service.impl.OrderServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class OrderTimeoutConsumer {

    private final OrderServiceImpl orderService;

    public OrderTimeoutConsumer(OrderServiceImpl orderService) {
        this.orderService = orderService;
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_DLX_QUEUE, ackMode = "MANUAL")
    public void handleTimeoutOrder(Message message, Channel channel) {
        String orderNo = new String(message.getBody());
        log.info("收到超时取消消息: orderNo={}", orderNo);

        try {
            orderService.timeoutCancel(orderNo);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            log.info("订单超时取消成功: orderNo={}", orderNo);
        } catch (Exception e) {
            log.error("处理超时取消失败: orderNo={}", orderNo, e);
            try {
                // Reject and don't requeue — dead letter is final
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
            } catch (IOException ioException) {
                log.error("消息拒绝失败", ioException);
            }
        }
    }
}
