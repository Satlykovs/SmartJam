package com.smartjam.smartjamanalyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Entry point that starts the Spring Boot application */
@SpringBootApplication
@EnableJpaAuditing
public class SmartjamAnalyzerApplication {

    /**
     * Main method to start the application.
     *
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        SpringApplication.run(SmartjamAnalyzerApplication.class, args);
    }
}
