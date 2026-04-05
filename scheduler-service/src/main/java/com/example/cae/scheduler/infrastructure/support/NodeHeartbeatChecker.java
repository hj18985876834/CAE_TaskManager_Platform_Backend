package com.example.cae.scheduler.infrastructure.support;

import com.example.cae.scheduler.application.service.NodeAppService;
import com.example.cae.scheduler.domain.model.ComputeNode;
import com.example.cae.scheduler.infrastructure.client.TaskClient;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class NodeHeartbeatChecker {
	private final NodeAppService nodeAppService;
	private final TaskClient taskClient;

	public NodeHeartbeatChecker(NodeAppService nodeAppService, TaskClient taskClient) {
		this.nodeAppService = nodeAppService;
		this.taskClient = taskClient;
	}

	public void markOfflineNodes() {
		List<ComputeNode> onlineNodes = nodeAppService.listOnlineNodes();
		LocalDateTime threshold = LocalDateTime.now().minusSeconds(30);
		for (ComputeNode node : onlineNodes) {
			if (node.getLastHeartbeatTime() == null || node.getLastHeartbeatTime().isBefore(threshold)) {
				taskClient.markNodeOfflineTasksFailed(node.getId(),
						"node heartbeat timeout, scheduler marked node offline: " + node.getNodeCode());
				nodeAppService.markNodeOffline(node.getNodeCode());
			}
		}
	}
}
