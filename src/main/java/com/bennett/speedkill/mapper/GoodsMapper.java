package com.bennett.speedkill.mapper;

import com.bennett.speedkill.entity.Goods;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Update;

/**
 * <p>
 * 商品表 Mapper 接口
 * </p>
 *
 * @author bennett
 * @since 2020-11-04
 */
public interface GoodsMapper extends BaseMapper<Goods> {

    @Update("update goods set sale = sale + 1 where id = #{id} and sale < stock")
    boolean updateSale(Long id);
}
