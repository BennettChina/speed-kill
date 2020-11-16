package com.bennett.speedkill.service;

import com.bennett.speedkill.entity.Goods;
import com.bennett.speedkill.entity.OrderInfo;

/**
 * 消息队列发送服务service接口
 *
 * @author bennett
 * @date 2020/11/12
 */

public interface MessageQueueService {

    /**
     * 减库存
     *
     * @param goodsId {@link Goods#getId()}
     */
    void decrStock(Long goodsId);

    /**
     * 创建订单
     *
     * @param orderInfo {@link OrderInfo}
     */
    void createOrder(OrderInfo orderInfo);
}
