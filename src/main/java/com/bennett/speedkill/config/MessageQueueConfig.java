package com.bennett.speedkill.config;

import com.bennett.speedkill.util.Constant;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MQ的队列配置
 *
 * @author bennett
 * @date 2020/11/12
 */

@Configuration
public class MessageQueueConfig {

    @Bean
    public Queue decrStockQueue() {
        return new Queue(Constant.GOODS_STOCK);
    }

    @Bean
    public Queue createOrderQueue() {
        return new Queue(Constant.CREATE_ORDER);
    }
}
