package com.bennett.speedkill;

import com.bennett.speedkill.entity.Goods;
import com.bennett.speedkill.service.IGoodsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@SpringBootTest
class SpeedKillApplicationTests {

    @Autowired
    private IGoodsService goodsService;

    @Test
    void contextLoads() {
    }

    /**
     * 往数据库里生成100万条数据
     */
    @Test
    public void generateGoods() {
        Random random = new Random();
        List<Goods> goodsList = new LinkedList<>();
        for (int i = 0; i < 1000000; i++) {
            Goods goods = new Goods();
            goods.setSale(0)
                    .setName(UUID.randomUUID().toString().replaceAll("-", ""))
                    .setStock(random.nextInt(10000) + 1)
                    .setStatus((byte) 0)
                    .setCreateTime(LocalDateTime.now());
            goodsList.add(goods);
        }

        goodsService.saveBatch(goodsList);
    }
}
