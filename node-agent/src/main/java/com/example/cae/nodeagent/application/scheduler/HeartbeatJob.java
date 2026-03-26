package com.example.cae.nodeagent.application.scheduler;

import com.example.cae.nodeagent.application.service.HeartbeatAppService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class HeartbeatJob {
	private final HeartbeatAppService heartbeatAppService;

	public HeartbeatJob(HeartbeatAppService heartbeatAppService) {
		this.heartbeatAppService = heartbeatAppService;
	}

	@Scheduled(fixedDelayString = "${cae.node.heartbeat-interval-ms:10000}")
	public void sendHeartbeat() {
		heartbeatAppService.sendHeartbeat();
	}
}

