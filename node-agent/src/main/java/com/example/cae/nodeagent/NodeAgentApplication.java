package com.example.cae.nodeagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.example.cae")
@EnableScheduling
public class NodeAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(NodeAgentApplication.class, args);
    }
}

