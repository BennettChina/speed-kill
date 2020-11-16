package com.bennett.speedkill.interceptor;

import com.bennett.speedkill.annotation.RequestLimit;
import com.bennett.speedkill.util.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 拦截处理秒杀系统的接口统一性问题
 *
 * @author bennett
 * @date 2020/11/11
 */

@Slf4j
@Component
public class SpeedKillInterceptor implements HandlerInterceptor {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //HandlerMethod 封装方法定义相关的信息,如类,方法,参数等
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();
        RequestLimit methodAnnotation = method.getAnnotation(RequestLimit.class);
        RequestLimit classAnnotation = method.getClass().getAnnotation(RequestLimit.class);

        // 如果注解没在方法上找到就使用类上的注解
        RequestLimit requestLimit = methodAnnotation == null ? classAnnotation : methodAnnotation;
        if (requestLimit == null) {
            return true;
        }

        if (isLimit(request, requestLimit)) {
            responseOut(response, "访问太频繁，累死宝宝了~");
            log.info("访问太频繁了...");
            return false;
        }
        return true;
    }

    /**
     * 把响应信息输出
     *
     * @param response {@link HttpServletResponse}
     * @param respStr  响应的字符串
     * @throws IOException io流异常
     */
    private void responseOut(HttpServletResponse response, String respStr) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        PrintWriter out = response.getWriter();
        out.append(respStr);
    }

    /**
     * 判断接口是否受限
     *
     * @param request      {@link HttpServletRequest}
     * @param requestLimit {@link RequestLimit}
     * @return {@link Boolean boolean value}
     */
    private boolean isLimit(HttpServletRequest request, RequestLimit requestLimit) {
        // sessionId可以换成token等唯一值
        String limitKey = request.getServletPath() + request.getSession().getId();
        Integer redisCount = (Integer) redisTemplate.opsForValue().get(Constant.LIMIT_PREFIX + limitKey);
        if (redisCount == null) {
            //初始 次数
            redisTemplate.opsForValue().set(Constant.LIMIT_PREFIX + limitKey, 1, requestLimit.time(), TimeUnit.SECONDS);
            return false;
        }

        // 次数超限
        if (redisCount >= requestLimit.maxCount()) {
            return true;
        }

        // 次数自增
        redisTemplate.opsForValue().increment(Constant.LIMIT_PREFIX + limitKey);
        return false;
    }
}
