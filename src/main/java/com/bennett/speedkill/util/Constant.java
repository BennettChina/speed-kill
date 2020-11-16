package com.bennett.speedkill.util;

/**
 * 系统常量
 *
 * @author bennett
 * @date 2020/11/4
 */

public class Constant {
    /**
     * 毫秒级日期时间样式
     */
    public static final String DATE_TIME_MILLIS = "yyyy-MM-dd HH:mm:ss:SSS";
    /**
     * 默认日期时间样式
     */
    public static final String DATE_TIME_DEFAULT = "yyyy-MM-dd HH:mm:ss";

    /**
     * 秒杀使用的redisson锁key
     */
    public static final String LOCK_KEY_SPEED_KILL = "speed-kill";

    /**
     * 商品库存的key{@code GOODS-{ID}}
     */
    public static final String GOODS_STOCK_PREFIX = "GOODS-";
    /**
     * 接口防刷的limitKey
     */
    public static final String LIMIT_PREFIX = "LIMIT-";

    // ===================MQ Queue Constant=================== //

    /**
     * 库存队列
     */
    public static final String GOODS_STOCK = "GOODS_STOCK";
    /**
     * 订单创建队列
     */
    public static final String CREATE_ORDER = "CREATE_ORDER";
}
