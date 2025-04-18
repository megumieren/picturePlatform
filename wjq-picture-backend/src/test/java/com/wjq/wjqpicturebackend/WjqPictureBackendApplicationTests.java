package com.wjq.wjqpicturebackend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;


@SpringBootTest
class WjqPictureBackendApplicationTests {

    @Test
    void contextLoads() {
        String property = System.getProperty("user.dir");

        System.out.println(property);
    }
}
