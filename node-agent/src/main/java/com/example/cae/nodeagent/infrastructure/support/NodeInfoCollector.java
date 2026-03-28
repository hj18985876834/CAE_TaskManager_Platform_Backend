package com.example.cae.nodeagent.infrastructure.support;

import com.example.cae.nodeagent.config.NodeAgentConfig;
import com.example.cae.nodeagent.domain.model.NodeInfo;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.util.List;

@Component
public class NodeInfoCollector {
	private final NodeAgentConfig nodeAgentConfig;

	public NodeInfoCollector(NodeAgentConfig nodeAgentConfig) {
		this.nodeAgentConfig = nodeAgentConfig;
	}

	public NodeInfo collectNodeInfo() {
		NodeInfo info = new NodeInfo();
		info.setNodeCode(nodeAgentConfig.getNodeCode());
		info.setNodeName(nodeAgentConfig.getNodeName());
		info.setHost(resolveHostWithPort());
		info.setMaxConcurrency(nodeAgentConfig.getMaxConcurrency());
		info.setCpuUsage(BigDecimal.ZERO);
		info.setMemoryUsage(BigDecimal.ZERO);
		info.setRunningCount(0);
		info.setSolverIds(nodeAgentConfig.getSolverIds() == null ? List.of(0L) : nodeAgentConfig.getSolverIds());
		return info;
	}

	private String resolveHostWithPort() {
		String host;
		try {
			host = InetAddress.getLocalHost().getHostAddress();
		} catch (Exception ex) {
			host = "127.0.0.1";
		}
		return host + ":" + nodeAgentConfig.getNodePort();
	}
}

