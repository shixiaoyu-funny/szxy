package com.shixiaoyu.xiangyueproject;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@MapperScan("com.shixiaoyu.xiangyueproject.mapper")
public class XiangyueProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(XiangyueProjectApplication.class, args);
    }

}
