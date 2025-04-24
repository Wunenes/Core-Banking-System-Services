package org.transactionMicroservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("org.transactionMicroservice")
public class TransactionMicroservice {
    public static void main(String[] args) {
        SpringApplication.run(TransactionMicroservice.class, args);
    }
}