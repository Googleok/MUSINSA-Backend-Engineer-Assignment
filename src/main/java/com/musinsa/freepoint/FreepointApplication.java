package com.musinsa.freepoint;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class FreepointApplication {

    public static void main(String[] args) {
        SpringApplication.run(FreepointApplication.class, args);
    }
}
