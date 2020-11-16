package com.bennett.speedkill.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.bennett.speedkill.annotation.RequestLimit;
import com.bennett.speedkill.entity.Goods;
import com.bennett.speedkill.service.IGoodsService;
import com.bennett.speedkill.util.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀API
 *
 * @author bennett
 * @date 2020/11/2
 */

@Slf4j
@RestController
@RequestMapping("/api/speedKill")
public class SpeedKillController {

    @Autowired
    private IGoodsService goodsService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 库存卖完标志
     */
    private static final Map<String, Boolean> IS_SALE_OVER = new ConcurrentHashMap<>(16);

    @RequestLimit
    @SentinelResource(value = "speed-kill", fallback = "秒杀失败，请重试!")
    @PostMapping
    public String killAction(@RequestBody Goods goods) {
        // todo 设置秒杀活动的结束标志（可放到redis中）

        if (IS_SALE_OVER.getOrDefault(Constant.GOODS_STOCK_PREFIX + goods.getId(), false)) {
            log.error("SaleOver is true, Thread is [{}]", Thread.currentThread().getName());
            return "秒杀失败";
        }

        // redis预减库存
        Long stock = redisTemplate.opsForValue().decrement(Constant.GOODS_STOCK_PREFIX + goods.getId());
        if (stock == null || stock.compareTo(0L) < 0) {
            redisTemplate.opsForValue().increment(Constant.GOODS_STOCK_PREFIX + goods.getId());
            IS_SALE_OVER.put(Constant.GOODS_STOCK_PREFIX + goods.getId(), true);
            // todo 发消息给集群上的其他机器，通知它们修改变量 IS_SALE_OVER=true
            log.error("秒杀失败, redis stock is [{}], Thread is [{}]", stock, Thread.currentThread().getName());
            return "秒杀失败";
        }

        // 数据库扣库存，生成订单
        // todo 存在分布式事务问题，消息失败后还需要redis库存回退，jvm标志重置
        goodsService.decStock(goods.getId());

        log.info("秒杀成功! Thread is [{}]", Thread.currentThread().getName());
        return "秒杀成功!";
    }

    @PostConstruct
    public void init() {
        // controller初始化时初始化redis中的库存
        Goods goods = goodsService.getOne(Wrappers.<Goods>lambdaQuery().eq(Goods::getId, 1326718786789318658L));
        redisTemplate.opsForValue().set(Constant.GOODS_STOCK_PREFIX + goods.getId(), String.valueOf(goods.getStock() - goods.getSale()), 1, TimeUnit.DAYS);
        log.info("init redis stock: [{}: {}]", Constant.GOODS_STOCK_PREFIX + goods.getId(), goods.getStock() - goods.getSale());
    }
}
