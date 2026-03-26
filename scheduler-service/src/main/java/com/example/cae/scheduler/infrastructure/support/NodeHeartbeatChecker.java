package com.example.cae.scheduler.infrastructure.support;

import com.example.cae.scheduler.application.service.NodeAppService;
import com.example.cae.scheduler.domain.model.ComputeNode;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class NodeHeartbeatChecker {
	private final NodeAppService nodeAppService;

	public NodeHeartbeatChecker(NodeAppService nodeAppService) {
		this.nodeAppService = nodeAppService;
	}

	public void markOfflineNodes() {
		List<ComputeNode> onlineNodes = nodeAppService.listOnlineNodes();
		LocalDateTime threshold = LocalDateTime.now().minusSeconds(30);
		for (ComputeNode node : onlineNodes) {
			if (node.getLastHeartbeatTime() == null || node.getLastHeartbeatTime().isBefore(threshold)) {
				nodeAppService.markNodeOffline(node.getNodeCode());
			}
		}
	}
}

