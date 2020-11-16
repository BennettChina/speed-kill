package com.bennett.speedkill.service;

import com.bennett.speedkill.entity.Goods;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 商品表 服务类
 * </p>
 *
 * @author bennett
 * @since 2020-11-04
 */
public interface IGoodsService extends IService<Goods> {

    /**
     * 更新卖出的商品数量
     *
     * @param id 商品ID
     */
    void decStock(Long id);
}
