package com.accountMicroservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.account_microservice")
public class AccountMicroservice {
    public static void main(String[] args) {
        SpringApplication.run(AccountMicroservice.class, args);
    }
}