package com.TransactionService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.transactionMicroservice")
public class TransactionService {
    public static void main(String[] args) {
        SpringApplication.run(TransactionService.class, args);
    }
}