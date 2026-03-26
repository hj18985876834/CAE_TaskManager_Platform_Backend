package com.example.cae.nodeagent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class NodeAgentBeanConfig {
	@Bean
	public Executor taskExecutor(NodeAgentConfig nodeAgentConfig) {
		int maxConcurrency = nodeAgentConfig.getMaxConcurrency() == null || nodeAgentConfig.getMaxConcurrency() <= 0
				? 2
				: nodeAgentConfig.getMaxConcurrency();
		org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor executor = new org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor();
		executor.setCorePoolSize(Math.max(1, maxConcurrency));
		executor.setMaxPoolSize(Math.max(1, maxConcurrency));
		executor.setQueueCapacity(200);
		executor.setThreadNamePrefix("node-agent-task-");
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		executor.initialize();
		return executor;
	}

	@Bean
	public RestTemplate restTemplate() {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(3000);
		requestFactory.setReadTimeout(10000);
		return new RestTemplate(requestFactory);
	}
}