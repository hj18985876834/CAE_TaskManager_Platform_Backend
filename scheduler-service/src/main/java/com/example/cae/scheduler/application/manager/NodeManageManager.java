package com.example.cae.scheduler.application.manager;

import com.example.cae.common.response.PageResult;
import com.example.cae.scheduler.application.service.NodeAppService;
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
public class NodeManageManager {
	private final NodeAppService nodeAppService;

	public NodeManageManager(NodeAppService nodeAppService) {
		this.nodeAppService = nodeAppService;
	}

	public Long registerNode(NodeRegisterRequest request) {
		return nodeAppService.registerNode(request);
	}

	public Long registerNodeFromAgent(NodeAgentRegisterRequest request) {
		return nodeAppService.registerNodeFromAgent(request);
	}

	public void heartbeat(NodeHeartbeatRequest request) {
		nodeAppService.heartbeat(request);
	}

	public void heartbeat(NodeHeartbeatRequest request, String nodeToken) {
		nodeAppService.heartbeat(request, nodeToken);
	}

	public PageResult<NodeDetailResponse> pageNodes(NodePageQueryRequest request) {
		return nodeAppService.pageNodes(request);
	}

	public NodeDetailResponse getNodeDetail(Long nodeId) {
		return nodeAppService.getNodeDetail(nodeId);
	}

	public void updateNodeStatus(Long nodeId, NodeStatusUpdateRequest request) {
		nodeAppService.updateNodeStatus(nodeId, request);
	}

	public List<NodeSolverResponse> listNodeSolvers(Long nodeId) {
		return nodeAppService.listNodeSolvers(nodeId);
	}

	public String getNodeToken(Long nodeId) {
		return nodeAppService.getNodeToken(nodeId);
	}
}

