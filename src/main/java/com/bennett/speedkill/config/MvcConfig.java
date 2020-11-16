package com.bennett.speedkill.config;

import com.bennett.speedkill.interceptor.SpeedKillInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author bennett
 * @date 2020/11/11
 */

@Component
public class MvcConfig implements WebMvcConfigurer {

    @Autowired
    private SpeedKillInterceptor speedKillInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(speedKillInterceptor)
        .addPathPatterns("/api/speedKill/**");
    }
}
