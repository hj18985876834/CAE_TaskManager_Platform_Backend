package com.example.cae.scheduler.application.facade;

import com.example.cae.common.response.PageResult;
import com.example.cae.scheduler.application.manager.NodeManageManager;
import com.example.cae.scheduler.interfaces.request.NodeAgentRegisterRequest;
import com.example.cae.scheduler.interfaces.request.NodeHeartbeatRequest;
import com.example.cae.scheduler.interfaces.request.NodePageQueryRequest;
import com.example.cae.scheduler.interfaces.request.NodeRegisterRequest;
import com.example.cae.scheduler.interfaces.request.UpdateNodeSolverStatusRequest;
import com.example.cae.scheduler.interfaces.request.UpdateNodeStatusRequest;
import com.example.cae.scheduler.interfaces.response.NodeDetailResponse;
import com.example.cae.scheduler.interfaces.response.NodeListItemResponse;
import com.example.cae.scheduler.interfaces.response.NodeSolverResponse;
import com.example.cae.scheduler.interfaces.response.NodeSolverStatusResponse;
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

	public void heartbeat(NodeHeartbeatRequest request, String nodeToken) {
		nodeManageManager.heartbeat(request, nodeToken);
	}

	public PageResult<NodeListItemResponse> pageNodes(NodePageQueryRequest request) {
		return nodeManageManager.pageNodes(request);
	}

	public NodeDetailResponse getNodeDetail(Long nodeId) {
		return nodeManageManager.getNodeDetail(nodeId);
	}

	public void updateNodeStatus(Long nodeId, UpdateNodeStatusRequest request) {
		nodeManageManager.updateNodeStatus(nodeId, request);
	}

	public NodeSolverStatusResponse updateNodeSolverStatus(Long nodeId, Long solverId, UpdateNodeSolverStatusRequest request) {
		return nodeManageManager.updateNodeSolverStatus(nodeId, solverId, request);
	}

	public List<NodeSolverResponse> listNodeSolvers(Long nodeId) {
		return nodeManageManager.listNodeSolvers(nodeId);
	}

	public String getNodeToken(Long nodeId) {
		return nodeManageManager.getNodeToken(nodeId);
	}
}
