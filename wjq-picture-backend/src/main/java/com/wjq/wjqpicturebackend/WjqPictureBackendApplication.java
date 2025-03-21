package com.wjq.wjqpicturebackend;

import org.apache.shardingsphere.spring.boot.ShardingSphereAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;


@SpringBootApplication(exclude = {ShardingSphereAutoConfiguration.class})
@MapperScan("com.wjq.wjqpicturebackend.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
public class WjqPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(WjqPictureBackendApplication.class, args);
    }

}
