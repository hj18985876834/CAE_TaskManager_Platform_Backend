package com.example.cae.scheduler.infrastructure.support;

import com.example.cae.scheduler.application.service.NodeAppService;
import com.example.cae.scheduler.domain.model.ComputeNode;
import com.example.cae.scheduler.infrastructure.client.TaskClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class NodeHeartbeatChecker {
	private static final Logger log = LoggerFactory.getLogger(NodeHeartbeatChecker.class);
	private final NodeAppService nodeAppService;
	private final TaskClient taskClient;

	public NodeHeartbeatChecker(NodeAppService nodeAppService, TaskClient taskClient) {
		this.nodeAppService = nodeAppService;
		this.taskClient = taskClient;
	}

	public void markOfflineNodes() {
		LocalDateTime threshold = LocalDateTime.now().minusSeconds(30);
		Set<Long> processedNodeIds = new HashSet<>();
		processStaleOnlineNodes(threshold, processedNodeIds);
		processStaleOfflineNodes(threshold, processedNodeIds);
	}

	private void processStaleOnlineNodes(LocalDateTime threshold, Set<Long> processedNodeIds) {
		List<ComputeNode> onlineNodes = nodeAppService.listOnlineNodes();
		for (ComputeNode node : onlineNodes) {
			if (!isNodeHeartbeatStale(node, threshold)) {
				continue;
			}
			nodeAppService.markNodeOffline(node.getNodeCode());
			triggerOfflineCompensation(node, "node heartbeat timeout, scheduler marked node offline: " + node.getNodeCode());
			processedNodeIds.add(node.getId());
		}
	}

	private void processStaleOfflineNodes(LocalDateTime threshold, Set<Long> processedNodeIds) {
		List<ComputeNode> offlineNodes = nodeAppService.listOfflineNodes();
		for (ComputeNode node : offlineNodes) {
			if (node == null || node.getId() == null || processedNodeIds.contains(node.getId())) {
				continue;
			}
			if (!isNodeHeartbeatStale(node, threshold)) {
				continue;
			}
			triggerOfflineCompensation(node, "node remains offline, retry compensation: " + node.getNodeCode());
			processedNodeIds.add(node.getId());
		}
	}

	private boolean isNodeHeartbeatStale(ComputeNode node, LocalDateTime threshold) {
		return node != null
				&& (node.getLastHeartbeatTime() == null || node.getLastHeartbeatTime().isBefore(threshold));
	}

	private void triggerOfflineCompensation(ComputeNode node, String reason) {
		int changedCount = taskClient.markNodeOfflineTasksFailed(node.getId(), reason);
		log.info("node offline compensation completed, nodeId={}, nodeCode={}, changedCount={}",
				node.getId(), node.getNodeCode(), changedCount);
	}
}
