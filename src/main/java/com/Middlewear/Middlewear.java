package com.Middlewear;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan
public class Middlewear {
    public static void main(String[] args){
        SpringApplication.run(Middlewear.class, args);
    }
}
