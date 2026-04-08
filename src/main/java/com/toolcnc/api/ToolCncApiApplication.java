package com.toolcnc.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class ToolCncApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ToolCncApiApplication.class, args);
    }
}
