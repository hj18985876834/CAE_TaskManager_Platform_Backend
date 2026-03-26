package com.example.cae.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.AntPathMatcher;

@Configuration
public class GatewayBeanConfig {
	@Bean
	public AntPathMatcher antPathMatcher() {
		return new AntPathMatcher();
	}
}