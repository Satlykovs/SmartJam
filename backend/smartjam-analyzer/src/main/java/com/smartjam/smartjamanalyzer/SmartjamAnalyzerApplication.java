package com.smartjam.smartjamanalyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point. Just starts the app Starts the Spring Boot application context. */
@SpringBootApplication
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
