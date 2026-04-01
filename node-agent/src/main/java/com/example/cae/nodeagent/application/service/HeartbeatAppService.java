package com.example.cae.nodeagent.application.service;

import com.example.cae.nodeagent.domain.model.NodeInfo;
import com.example.cae.nodeagent.infrastructure.client.SchedulerNodeClient;
import com.example.cae.nodeagent.infrastructure.support.NodeInfoCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class HeartbeatAppService {
	private static final Logger log = LoggerFactory.getLogger(HeartbeatAppService.class);

	private final SchedulerNodeClient schedulerNodeClient;
	private final NodeInfoCollector nodeInfoCollector;

	public HeartbeatAppService(SchedulerNodeClient schedulerNodeClient, NodeInfoCollector nodeInfoCollector) {
		this.schedulerNodeClient = schedulerNodeClient;
		this.nodeInfoCollector = nodeInfoCollector;
	}

	public void sendHeartbeat() {
		NodeInfo nodeInfo = nodeInfoCollector.collectNodeInfo();
		try {
			schedulerNodeClient.heartbeat(nodeInfo);
		} catch (Exception ex) {
			log.warn("heartbeat failed, retry register node: {}", ex.getMessage());
			try {
				schedulerNodeClient.register(nodeInfo);
				schedulerNodeClient.heartbeat(nodeInfoCollector.collectNodeInfo());
				log.info("node re-register and heartbeat retry succeeded");
			} catch (Exception retryEx) {
				log.warn("heartbeat retry after register failed: {}", retryEx.getMessage());
			}
		}
	}
}
