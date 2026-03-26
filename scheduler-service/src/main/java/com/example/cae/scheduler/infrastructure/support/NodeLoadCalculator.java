package com.example.cae.scheduler.infrastructure.support;

import com.example.cae.scheduler.domain.model.ComputeNode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class NodeLoadCalculator {
	public BigDecimal calcScore(ComputeNode node) {
		BigDecimal running = BigDecimal.valueOf(node.getRunningCount() == null ? 0 : node.getRunningCount());
		BigDecimal cpu = node.getCpuUsage() == null ? BigDecimal.ZERO : node.getCpuUsage();
		BigDecimal memory = node.getMemoryUsage() == null ? BigDecimal.ZERO : node.getMemoryUsage();
		return running.multiply(BigDecimal.valueOf(10)).add(cpu).add(memory);
	}
}

