package com.bennett.speedkill.service.impl;

import com.bennett.speedkill.entity.OrderInfo;
import com.bennett.speedkill.service.MessageQueueService;
import com.bennett.speedkill.util.Constant;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 消息队列发送服务的实现类-由rabbitMQ实现
 *
 * @author bennett
 * @date 2020/11/12
 */

@Service
public class MessageQueueServiceImpl implements MessageQueueService {

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Override
    public void decrStock(Long goodsId) {
        amqpTemplate.convertAndSend(Constant.GOODS_STOCK, goodsId);
    }

    @Override
    public void createOrder(OrderInfo orderInfo) {
        amqpTemplate.convertAndSend(Constant.CREATE_ORDER, orderInfo);
    }
}
