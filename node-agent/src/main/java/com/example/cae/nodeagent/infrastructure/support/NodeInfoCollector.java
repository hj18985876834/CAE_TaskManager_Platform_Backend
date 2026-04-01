package com.example.cae.nodeagent.infrastructure.support;

import com.example.cae.nodeagent.config.NodeAgentConfig;
import com.example.cae.nodeagent.application.manager.TaskRuntimeRegistry;
import com.example.cae.nodeagent.domain.model.NodeInfo;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.util.List;

@Component
public class NodeInfoCollector {
	private final NodeAgentConfig nodeAgentConfig;
	private final TaskRuntimeRegistry taskRuntimeRegistry;

	public NodeInfoCollector(NodeAgentConfig nodeAgentConfig, TaskRuntimeRegistry taskRuntimeRegistry) {
		this.nodeAgentConfig = nodeAgentConfig;
		this.taskRuntimeRegistry = taskRuntimeRegistry;
	}

	public NodeInfo collectNodeInfo() {
		NodeInfo info = new NodeInfo();
		info.setNodeCode(nodeAgentConfig.getNodeCode());
		info.setNodeName(nodeAgentConfig.getNodeName());
		info.setHost(resolveHostWithPort());
		info.setMaxConcurrency(nodeAgentConfig.getMaxConcurrency());
		info.setCpuUsage(BigDecimal.ZERO);
		info.setMemoryUsage(BigDecimal.ZERO);
		info.setRunningCount(taskRuntimeRegistry.runningCount());
		info.setSolverIds(nodeAgentConfig.getSolverIds() == null ? List.of(0L) : nodeAgentConfig.getSolverIds());
		return info;
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
