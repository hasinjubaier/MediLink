package com.medilink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot Entry Point for MediLink 2.0.
 */
@SpringBootApplication
@EnableScheduling
public class MedilinkApplication {

    public static void main(String[] args) {
        SpringApplication.run(MedilinkApplication.class, args);
    }
}
