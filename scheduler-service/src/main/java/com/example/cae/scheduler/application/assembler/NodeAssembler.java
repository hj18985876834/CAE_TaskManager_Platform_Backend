package com.example.cae.scheduler.application.assembler;

import com.example.cae.scheduler.domain.model.ComputeNode;
import com.example.cae.scheduler.infrastructure.persistence.entity.ComputeNodePO;
import com.example.cae.scheduler.interfaces.request.NodeRegisterRequest;
import com.example.cae.scheduler.interfaces.response.NodeDetailResponse;

public final class NodeAssembler {
	private NodeAssembler() {
	}

	public static ComputeNode toNode(NodeRegisterRequest request) {
		ComputeNode node = new ComputeNode();
		node.setNodeCode(request.getNodeCode());
		node.setNodeName(request.getNodeName());
		node.setHost(request.getHost());
		node.setPort(request.getPort());
		node.setMaxConcurrency(request.getMaxConcurrency());
		return node;
	}

	public static NodeDetailResponse toDetailResponse(ComputeNode node) {
		NodeDetailResponse response = new NodeDetailResponse();
		response.setNodeId(node.getId());
		response.setNodeCode(node.getNodeCode());
		response.setNodeName(node.getNodeName());
		response.setHost(node.getHost());
		response.setPort(node.getPort());
		response.setStatus(node.getStatus());
		response.setMaxConcurrency(node.getMaxConcurrency());
		response.setRunningCount(node.getRunningCount());
		response.setCpuUsage(node.getCpuUsage());
		response.setMemoryUsage(node.getMemoryUsage());
		response.setLastHeartbeatTime(node.getLastHeartbeatTime());
		return response;
	}

	public static ComputeNode fromPO(ComputeNodePO po) {
		ComputeNode node = new ComputeNode();
		node.setId(po.getId());
		node.setNodeCode(po.getNodeCode());
		node.setNodeName(po.getNodeName());
		node.setHost(po.getHost());
		node.setStatus(po.getStatus());
		node.setMaxConcurrency(po.getMaxConcurrency());
		node.setRunningCount(po.getRunningCount());
		node.setCpuUsage(po.getCpuUsage());
		node.setMemoryUsage(po.getMemoryUsage());
		node.setLastHeartbeatTime(po.getLastHeartbeatTime());
		node.setCreatedAt(po.getCreatedAt());
		node.setUpdatedAt(po.getUpdatedAt());
		return node;
	}

	public static ComputeNodePO toPO(ComputeNode node) {
		ComputeNodePO po = new ComputeNodePO();
		po.setId(node.getId());
		po.setNodeCode(node.getNodeCode());
		po.setNodeName(node.getNodeName());
		po.setHost(node.getHost());
		po.setStatus(node.getStatus());
		po.setMaxConcurrency(node.getMaxConcurrency());
		po.setRunningCount(node.getRunningCount());
		po.setCpuUsage(node.getCpuUsage());
		po.setMemoryUsage(node.getMemoryUsage());
		po.setLastHeartbeatTime(node.getLastHeartbeatTime());
		po.setCreatedAt(node.getCreatedAt());
		po.setUpdatedAt(node.getUpdatedAt());
		return po;
	}
}
