package com.bennett.speedkill.service.impl;

import com.bennett.speedkill.entity.OrderInfo;
import com.bennett.speedkill.mapper.GoodsMapper;
import com.bennett.speedkill.mapper.OrderInfoMapper;
import com.bennett.speedkill.service.MessageReceiveService;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.annotation.RabbitListeners;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * MQ接收消息的实现
 *
 * @author bennett
 * @date 2020/11/12
 */

@Service
@RabbitListeners({@RabbitListener(queues = "GOODS_STOCK"), @RabbitListener(queues = "CREATE_ORDER")})
public class MessageReceiveServiceImpl implements MessageReceiveService {

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Override
    @RabbitHandler
    public void decrStock(Long goodsId) {
        goodsMapper.updateSale(goodsId);
    }

    @Override
    @RabbitHandler
    public void createOrder(OrderInfo orderInfo) {
        orderInfoMapper.insert(orderInfo);
    }
}
