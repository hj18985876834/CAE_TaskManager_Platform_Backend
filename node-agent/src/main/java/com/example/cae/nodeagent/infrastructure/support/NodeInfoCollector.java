package com.example.cae.nodeagent.infrastructure.support;

import com.example.cae.nodeagent.config.NodeAgentConfig;
import com.example.cae.nodeagent.application.manager.TaskRuntimeRegistry;
import com.example.cae.nodeagent.domain.model.NodeInfo;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.util.List;
import java.math.RoundingMode;

@Component
public class NodeInfoCollector {
	private final NodeAgentConfig nodeAgentConfig;
	private final TaskRuntimeRegistry taskRuntimeRegistry;
	private final com.sun.management.OperatingSystemMXBean operatingSystemMXBean;

	public NodeInfoCollector(NodeAgentConfig nodeAgentConfig, TaskRuntimeRegistry taskRuntimeRegistry) {
		this.nodeAgentConfig = nodeAgentConfig;
		this.taskRuntimeRegistry = taskRuntimeRegistry;
		java.lang.management.OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
		this.operatingSystemMXBean = bean instanceof com.sun.management.OperatingSystemMXBean osBean ? osBean : null;
	}

	public NodeInfo collectNodeInfo() {
		NodeInfo info = new NodeInfo();
		info.setNodeCode(nodeAgentConfig.getNodeCode());
		info.setNodeName(nodeAgentConfig.getNodeName());
		info.setHost(resolveHostWithPort());
		info.setMaxConcurrency(nodeAgentConfig.getMaxConcurrency());
		info.setCpuUsage(collectCpuUsage());
		info.setMemoryUsage(collectMemoryUsage());
		info.setRunningCount(taskRuntimeRegistry.runningCount());
		info.setSolverIds(nodeAgentConfig.getSolverIds() == null ? List.of() : nodeAgentConfig.getSolverIds());
		return info;
	}

	private BigDecimal collectCpuUsage() {
		if (operatingSystemMXBean == null) {
			return BigDecimal.ZERO;
		}
		double cpuLoad = operatingSystemMXBean.getCpuLoad();
		if (cpuLoad < 0) {
			return BigDecimal.ZERO;
		}
		return toPercent(cpuLoad);
	}

	private BigDecimal collectMemoryUsage() {
		if (operatingSystemMXBean == null) {
			return BigDecimal.ZERO;
		}
		long total = operatingSystemMXBean.getTotalMemorySize();
		long free = operatingSystemMXBean.getFreeMemorySize();
		if (total <= 0L) {
			return BigDecimal.ZERO;
		}
		long used = Math.max(0L, total - Math.max(0L, free));
		return toPercent((double) used / (double) total);
	}

	private BigDecimal toPercent(double ratio) {
		double normalized = Math.max(0D, Math.min(1D, ratio));
		return BigDecimal.valueOf(normalized)
				.multiply(BigDecimal.valueOf(100))
				.setScale(2, RoundingMode.HALF_UP);
	}

	private String resolveHostWithPort() {
		if (nodeAgentConfig.getAdvertisedHost() != null && !nodeAgentConfig.getAdvertisedHost().isBlank()) {
			return nodeAgentConfig.getAdvertisedHost();
		}
		String host;
		try {
			host = InetAddress.getLocalHost().getHostAddress();
		} catch (Exception ex) {
			host = "127.0.0.1";
		}
		return host + ":" + nodeAgentConfig.getNodePort();
	}
}
