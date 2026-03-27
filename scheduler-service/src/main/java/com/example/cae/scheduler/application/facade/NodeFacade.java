package com.example.cae.scheduler.application.facade;

import com.example.cae.common.response.PageResult;
import com.example.cae.scheduler.application.manager.NodeManageManager;
import com.example.cae.scheduler.interfaces.request.NodeAgentRegisterRequest;
import com.example.cae.scheduler.interfaces.request.NodeHeartbeatRequest;
import com.example.cae.scheduler.interfaces.request.NodePageQueryRequest;
import com.example.cae.scheduler.interfaces.request.NodeRegisterRequest;
import com.example.cae.scheduler.interfaces.request.NodeStatusUpdateRequest;
import com.example.cae.scheduler.interfaces.response.NodeDetailResponse;
import com.example.cae.scheduler.interfaces.response.NodeSolverResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NodeFacade {
	private final NodeManageManager nodeManageManager;

	public NodeFacade(NodeManageManager nodeManageManager) {
		this.nodeManageManager = nodeManageManager;
	}

	public Long registerNode(NodeRegisterRequest request) {
		return nodeManageManager.registerNode(request);
	}

	public Long registerNodeFromAgent(NodeAgentRegisterRequest request) {
		return nodeManageManager.registerNodeFromAgent(request);
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

	public void updateNodeStatus(Long nodeId, NodeStatusUpdateRequest request) {
		nodeManageManager.updateNodeStatus(nodeId, request);
	}

	public List<NodeSolverResponse> listNodeSolvers(Long nodeId) {
		return nodeManageManager.listNodeSolvers(nodeId);
	}
}
