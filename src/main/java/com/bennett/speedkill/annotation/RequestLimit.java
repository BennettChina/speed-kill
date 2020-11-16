package com.bennett.speedkill.annotation;

import java.lang.annotation.*;

/**
 * 接口防刷的注解, 通过设置间隔时间、最大请求次数来防刷
 *
 * @author bennett
 * @date 2020/11/11
 */

@Documented
@Inherited
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestLimit {

    /**
     * 请求间隔时间，单位：秒. 默认为1秒
     *
     * @return 请求间隔时间
     */
    int time() default 1;

    /**
     * 单位间隔时间请求最大次数, 默认为1次
     *
     * @return 最大请求次数
     */
    int maxCount() default 1;
}
