package com.example.cae.scheduler.application.facade;

import com.example.cae.common.response.PageResult;
import com.example.cae.scheduler.application.manager.NodeManageManager;
import com.example.cae.scheduler.interfaces.request.NodeHeartbeatRequest;
import com.example.cae.scheduler.interfaces.request.NodePageQueryRequest;
import com.example.cae.scheduler.interfaces.request.NodeRegisterRequest;
import com.example.cae.scheduler.interfaces.response.NodeDetailResponse;
import org.springframework.stereotype.Component;

@Component
public class NodeFacade {
	private final NodeManageManager nodeManageManager;

	public NodeFacade(NodeManageManager nodeManageManager) {
		this.nodeManageManager = nodeManageManager;
	}

	public void registerNode(NodeRegisterRequest request) {
		nodeManageManager.registerNode(request);
	}

	public void heartbeat(NodeHeartbeatRequest request) {
		nodeManageManager.heartbeat(request);
	}

	public PageResult<NodeDetailResponse> pageNodes(NodePageQueryRequest request) {
		return nodeManageManager.pageNodes(request);
	}

	public NodeDetailResponse getNodeDetail(Long nodeId) {
		return nodeManageManager.getNodeDetail(nodeId);
	}
}
