package com.bennett.speedkill;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 应用启动类
 *
 * @author bennett
 * @date 2020/11/2
 */
@MapperScan("com.bennett.speedkill.mapper")
@SpringBootApplication
public class SpeedKillApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpeedKillApplication.class, args);
    }

}
