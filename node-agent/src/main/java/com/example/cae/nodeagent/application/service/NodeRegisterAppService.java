package com.example.cae.nodeagent.application.service;

import com.example.cae.nodeagent.domain.model.NodeInfo;
import com.example.cae.nodeagent.infrastructure.client.SchedulerNodeClient;
import com.example.cae.nodeagent.infrastructure.support.NodeInfoCollector;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NodeRegisterAppService {
	private static final Logger log = LoggerFactory.getLogger(NodeRegisterAppService.class);

	private final SchedulerNodeClient schedulerNodeClient;
	private final NodeInfoCollector nodeInfoCollector;

	public NodeRegisterAppService(SchedulerNodeClient schedulerNodeClient, NodeInfoCollector nodeInfoCollector) {
		this.schedulerNodeClient = schedulerNodeClient;
		this.nodeInfoCollector = nodeInfoCollector;
	}

	@PostConstruct
	public void registerOnStartup() {
		try {
			registerSelf();
		} catch (Exception ex) {
			log.warn("register node on startup failed: {}", ex.getMessage());
		}
	}

	public void registerSelf() {
		NodeInfo nodeInfo = nodeInfoCollector.collectNodeInfo();
		schedulerNodeClient.register(nodeInfo);
	}
}

