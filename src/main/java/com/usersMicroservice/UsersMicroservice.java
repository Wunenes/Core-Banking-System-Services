package com.usersMicroservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.usersMicroservice")
public class UsersMicroservice {
    public static void main(String[] args) {
        SpringApplication.run(UsersMicroservice.class, args);
    }
}
