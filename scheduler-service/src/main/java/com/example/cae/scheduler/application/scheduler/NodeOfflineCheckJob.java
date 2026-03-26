package com.example.cae.scheduler.application.scheduler;

import com.example.cae.scheduler.infrastructure.support.NodeHeartbeatChecker;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NodeOfflineCheckJob {
	private final NodeHeartbeatChecker nodeHeartbeatChecker;

	public NodeOfflineCheckJob(NodeHeartbeatChecker nodeHeartbeatChecker) {
		this.nodeHeartbeatChecker = nodeHeartbeatChecker;
	}

	@Scheduled(fixedDelayString = "${scheduler.node-offline-check-interval-ms:15000}")
	public void run() {
		nodeHeartbeatChecker.markOfflineNodes();
	}
}

