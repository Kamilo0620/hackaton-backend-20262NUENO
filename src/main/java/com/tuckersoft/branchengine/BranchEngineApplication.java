package com.tuckersoft.branchengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BranchEngineApplication {
    public static void main(String[] args) {
        SpringApplication.run(BranchEngineApplication.class, args);
    }
}
