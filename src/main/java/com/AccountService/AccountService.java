package com.AccountService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.accountMicroservice")
public class AccountService {
    public static void main(String[] args) {
        SpringApplication.run(AccountService.class, args);
    }
}