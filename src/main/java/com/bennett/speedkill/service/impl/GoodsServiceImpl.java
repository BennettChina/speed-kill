package com.bennett.speedkill.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bennett.speedkill.entity.Goods;
import com.bennett.speedkill.entity.OrderInfo;
import com.bennett.speedkill.mapper.GoodsMapper;
import com.bennett.speedkill.service.IGoodsService;
import com.bennett.speedkill.service.MessageQueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * <p>
 * 商品表 服务实现类
 * </p>
 *
 * @author bennett
 * @since 2020-11-04
 */
@Service
public class GoodsServiceImpl extends ServiceImpl<GoodsMapper, Goods> implements IGoodsService {

    @Autowired
    MessageQueueService messageQueueService;

    @Override
    public void decStock(Long id) {
        // 减库存
        messageQueueService.decrStock(id);

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setGoodsId(id).setNumber(1).setUserId(1L).setCreateTime(LocalDateTime.now()).setStatus((byte)0);
        // 创建订单
        messageQueueService.createOrder(orderInfo);
    }
}
