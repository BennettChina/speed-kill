package com.bennett.speedkill.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 公共工具类
 *
 * @author bennett
 * @date 2020/11/4
 */

public class CommonUtils {

    /**
     * 获取当前日期时间
     *
     * @return 当前日期时间，格式为 {@link Constant#DATE_TIME_MILLIS}
     */
    public static String getNow() {
        return getNow(Constant.DATE_TIME_MILLIS);
    }

    /**
     * 获取当前日期时间
     *
     * @return 当前日期时间
     */
    public static String getNow(String formatter) {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(formatter));
    }
}
