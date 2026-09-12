package com.ucm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class UcmApplication {

    public static void main(String[] args) {
        SpringApplication.run(UcmApplication.class, args);
    }
}
