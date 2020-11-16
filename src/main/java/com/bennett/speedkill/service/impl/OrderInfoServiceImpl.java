package com.bennett.speedkill.service.impl;

import com.bennett.speedkill.entity.OrderInfo;
import com.bennett.speedkill.mapper.OrderInfoMapper;
import com.bennett.speedkill.service.IOrderInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 用户订单表 服务实现类
 * </p>
 *
 * @author bennett
 * @since 2020-11-04
 */
@Service
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements IOrderInfoService {

}
