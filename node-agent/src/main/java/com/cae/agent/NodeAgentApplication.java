package com.cae.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NodeAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(NodeAgentApplication.class, args);
    }
}
