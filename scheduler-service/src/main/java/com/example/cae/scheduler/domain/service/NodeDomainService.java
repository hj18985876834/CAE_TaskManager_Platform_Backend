package com.example.cae.scheduler.domain.service;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.exception.BizException;
import com.example.cae.scheduler.domain.model.ComputeNode;
import com.example.cae.scheduler.interfaces.request.NodeHeartbeatRequest;
import com.example.cae.scheduler.interfaces.request.NodeRegisterRequest;
import org.springframework.stereotype.Service;

@Service
public class NodeDomainService {
	public void validateRegisterRequest(NodeRegisterRequest request) {
		if (request == null || isBlank(request.getNodeCode()) || isBlank(request.getNodeName()) || isBlank(request.getHost())) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "invalid register request");
		}
		if (request.getMaxConcurrency() == null || request.getMaxConcurrency() < 1) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "invalid maxConcurrency");
		}
		if (request.getPort() == null || request.getPort() < 1 || request.getPort() > 65535) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "invalid port");
		}
	}

	public void validateHeartbeatRequest(NodeHeartbeatRequest request) {
		if (request == null || request.getNodeId() == null
				|| request.getCpuUsage() == null || request.getMemoryUsage() == null || request.getRunningCount() == null
				|| request.getRunningCount() < 0) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "invalid heartbeat request");
		}
	}

	public boolean canDispatch(ComputeNode node) {
		return node != null && node.canDispatch();
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}
}
