package com.example.cae.scheduler.application.manager;

import com.example.cae.common.response.PageResult;
import com.example.cae.scheduler.application.service.NodeAppService;
import com.example.cae.scheduler.interfaces.request.NodeAgentRegisterRequest;
import com.example.cae.scheduler.interfaces.request.NodeHeartbeatRequest;
import com.example.cae.scheduler.interfaces.request.NodePageQueryRequest;
import com.example.cae.scheduler.interfaces.request.UpdateNodeSolverStatusRequest;
import com.example.cae.scheduler.interfaces.request.UpdateNodeStatusRequest;
import com.example.cae.scheduler.interfaces.response.NodeDetailResponse;
import com.example.cae.scheduler.interfaces.response.NodeListItemResponse;
import com.example.cae.scheduler.interfaces.response.NodeSolverResponse;
import com.example.cae.scheduler.interfaces.response.NodeSolverStatusResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NodeManageManager {
	private final NodeAppService nodeAppService;

	public NodeManageManager(NodeAppService nodeAppService) {
		this.nodeAppService = nodeAppService;
	}

	public Long registerNodeFromAgent(NodeAgentRegisterRequest request) {
		return nodeAppService.registerNodeFromAgent(request);
	}

	public void heartbeat(NodeHeartbeatRequest request, String nodeToken) {
		nodeAppService.heartbeat(request, nodeToken);
	}

	public PageResult<NodeListItemResponse> pageNodes(NodePageQueryRequest request) {
		return nodeAppService.pageNodes(request);
	}

	public NodeDetailResponse getNodeDetail(Long nodeId) {
		return nodeAppService.getNodeDetail(nodeId);
	}

	public void updateNodeStatus(Long nodeId, UpdateNodeStatusRequest request) {
		nodeAppService.updateNodeStatus(nodeId, request);
	}

	public NodeSolverStatusResponse updateNodeSolverStatus(Long nodeId, Long solverId, UpdateNodeSolverStatusRequest request) {
		return nodeAppService.updateNodeSolverStatus(nodeId, solverId, request);
	}

	public List<NodeSolverResponse> listNodeSolvers(Long nodeId) {
		return nodeAppService.listNodeSolvers(nodeId);
	}

	public String getNodeToken(Long nodeId) {
		return nodeAppService.getNodeToken(nodeId);
	}
}
