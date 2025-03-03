package com.wjq.wjqpicturebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.wjq.wjqpicturebackend.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
public class WjqPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(WjqPictureBackendApplication.class, args);
    }

}
