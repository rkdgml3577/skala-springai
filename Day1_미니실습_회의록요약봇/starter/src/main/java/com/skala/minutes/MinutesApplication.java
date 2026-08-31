package com.skala.minutes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry                 // @Retryable 을 쓰려면 이 한 줄이 있어야 한다
@SpringBootApplication
public class MinutesApplication {

    public static void main(String[] args) {
        SpringApplication.run(MinutesApplication.class, args);
    }
}
