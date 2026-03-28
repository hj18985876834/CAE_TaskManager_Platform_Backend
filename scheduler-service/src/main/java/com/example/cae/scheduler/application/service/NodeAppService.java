package com.example.cae.scheduler.application.service;

import com.example.cae.common.exception.BizException;
import com.example.cae.common.response.PageResult;
import com.example.cae.scheduler.application.assembler.NodeAssembler;
import com.example.cae.scheduler.domain.model.ComputeNode;
import com.example.cae.scheduler.domain.model.NodeSolverCapability;
import com.example.cae.scheduler.domain.repository.ComputeNodeRepository;
import com.example.cae.scheduler.domain.repository.NodeSolverCapabilityRepository;
import com.example.cae.scheduler.domain.service.NodeDomainService;
import com.example.cae.scheduler.interfaces.request.NodeAgentRegisterRequest;
import com.example.cae.scheduler.interfaces.request.NodeHeartbeatRequest;
import com.example.cae.scheduler.interfaces.request.NodePageQueryRequest;
import com.example.cae.scheduler.interfaces.request.NodeRegisterRequest;
import com.example.cae.scheduler.interfaces.request.NodeStatusUpdateRequest;
import com.example.cae.scheduler.interfaces.response.AvailableNodeResponse;
import com.example.cae.scheduler.interfaces.response.NodeDetailResponse;
import com.example.cae.scheduler.interfaces.response.NodeSolverResponse;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class NodeAppService {
	private final ComputeNodeRepository computeNodeRepository;
	private final NodeSolverCapabilityRepository nodeSolverCapabilityRepository;
	private final NodeDomainService nodeDomainService;

	public NodeAppService(ComputeNodeRepository computeNodeRepository,
						  NodeSolverCapabilityRepository nodeSolverCapabilityRepository,
						  NodeDomainService nodeDomainService) {
		this.computeNodeRepository = computeNodeRepository;
		this.nodeSolverCapabilityRepository = nodeSolverCapabilityRepository;
		this.nodeDomainService = nodeDomainService;
	}

	public Long registerNode(NodeRegisterRequest request) {
		nodeDomainService.validateRegisterRequest(request);
		Optional<ComputeNode> existingNode = computeNodeRepository.findByNodeCode(request.getNodeCode());
		if (existingNode.isPresent()) {
			ComputeNode node = existingNode.get();
			node.setNodeName(request.getNodeName());
			node.setHost(request.getHost());
			node.setMaxConcurrency(request.getMaxConcurrency());
			if (node.getNodeToken() == null || node.getNodeToken().isBlank()) {
				node.setNodeToken(generateNodeToken(node.getNodeCode()));
			}
			node.markOnline();
			node.setLastHeartbeatTime(LocalDateTime.now());
			computeNodeRepository.update(node);
			nodeSolverCapabilityRepository.replaceNodeCapabilities(node.getId(), request.getSolverIds());
			return node.getId();
		}

		ComputeNode node = NodeAssembler.toNode(request);
		node.setNodeToken(generateNodeToken(node.getNodeCode()));
		node.markOnline();
		node.setRunningCount(0);
		node.setLastHeartbeatTime(LocalDateTime.now());
		computeNodeRepository.save(node);
		nodeSolverCapabilityRepository.replaceNodeCapabilities(node.getId(), request.getSolverIds());
		return node.getId();
	}

	public Long registerNodeFromAgent(NodeAgentRegisterRequest request) {
		NodeRegisterRequest registerRequest = new NodeRegisterRequest();
		registerRequest.setNodeCode(request == null ? null : request.getNodeCode());
		registerRequest.setNodeName(request == null ? null : request.getNodeName());
		registerRequest.setHost(composeHost(request == null ? null : request.getHost()));
		registerRequest.setMaxConcurrency(request == null ? null : request.getMaxConcurrency());
		registerRequest.setSolverIds(extractSolverIds(request));

		Long nodeId = registerNode(registerRequest);
		List<NodeSolverCapability> capabilities = toCapabilities(nodeId, request);
		nodeSolverCapabilityRepository.replaceNodeCapabilitiesWithDetails(nodeId, capabilities);
		return nodeId;
	}

	public void heartbeat(NodeHeartbeatRequest request) {
		heartbeat(request, null);
	}

	public void heartbeat(NodeHeartbeatRequest request, String nodeToken) {
		nodeDomainService.validateHeartbeatRequest(request);
		ComputeNode node = resolveNode(request);
		if (nodeToken != null) {
			if (nodeToken.isBlank()) {
				throw new BizException(401, "node token required");
			}
			if (node.getNodeToken() == null || !nodeToken.equals(node.getNodeToken())) {
				throw new BizException(401, "invalid node token");
			}
		}
		node.refreshHeartbeat(request.getCpuUsage(), request.getMemoryUsage(), request.getRunningCount(), LocalDateTime.now());
		node.markOnline();
		computeNodeRepository.update(node);
	}

	public PageResult<NodeDetailResponse> pageNodes(NodePageQueryRequest request) {
		int pageNum = request == null || request.getPageNum() == null || request.getPageNum() < 1 ? 1 : request.getPageNum();
		int pageSize = request == null || request.getPageSize() == null || request.getPageSize() < 1 ? 10 : request.getPageSize();
		long offset = (long) (pageNum - 1) * pageSize;

		PageResult<ComputeNode> page = computeNodeRepository.page(request, offset, pageSize);
		List<NodeDetailResponse> records = page.getRecords().stream().map(this::toNodeDetail).toList();
		return PageResult.of(page.getTotal(), pageNum, pageSize, records);
	}

	public NodeDetailResponse getNodeDetail(Long nodeId) {
		ComputeNode node = computeNodeRepository.findById(nodeId).orElseThrow(() -> new BizException(404, "node not found"));
		return toNodeDetail(node);
	}

	public void updateNodeStatus(Long nodeId, NodeStatusUpdateRequest request) {
		if (nodeId == null || request == null || request.getStatus() == null || request.getStatus().isBlank()) {
			throw new BizException(400, "invalid node status request");
		}
		String status = request.getStatus().trim().toUpperCase();
		if (!Set.of("ONLINE", "OFFLINE", "DISABLED").contains(status)) {
			throw new BizException(400, "unsupported node status");
		}
		ComputeNode node = computeNodeRepository.findById(nodeId).orElseThrow(() -> new BizException(404, "node not found"));
		node.setStatus(status);
		computeNodeRepository.update(node);
	}

	public List<NodeSolverResponse> listNodeSolvers(Long nodeId) {
		if (nodeId == null) {
			throw new BizException(400, "nodeId is required");
		}
		computeNodeRepository.findById(nodeId).orElseThrow(() -> new BizException(404, "node not found"));
		return nodeSolverCapabilityRepository.listByNodeId(nodeId).stream()
				.filter(NodeSolverCapability::isEnabled)
				.map(capability -> {
					NodeSolverResponse response = new NodeSolverResponse();
					response.setSolverId(capability.getSolverId());
					response.setSolverVersion(capability.getSolverVersion());
					response.setEnabled(capability.getEnabled());
					return response;
				})
				.toList();
	}

	public void markNodeOffline(String nodeCode) {
		computeNodeRepository.findByNodeCode(nodeCode).ifPresent(node -> {
			node.markOffline();
			computeNodeRepository.update(node);
		});
	}

	public List<ComputeNode> listOnlineNodes() {
		return computeNodeRepository.listByStatus("ONLINE");
	}

	public List<AvailableNodeResponse> listAvailableNodes(Long solverId) {
		if (solverId == null) {
			return List.of();
		}
		Set<Long> capableNodeIds = new HashSet<>(nodeSolverCapabilityRepository.listBySolverId(solverId).stream()
				.filter(NodeSolverCapability::isEnabled)
				.map(NodeSolverCapability::getNodeId)
				.toList());

		return computeNodeRepository.listByStatus("ONLINE").stream()
				.filter(node -> capableNodeIds.contains(node.getId()))
				.filter(nodeDomainService::canDispatch)
				.map(this::toAvailableNodeResponse)
				.toList();
	}

	public void updateRunningCount(Long nodeId, Integer delta) {
		if (nodeId == null || delta == null || delta == 0) {
			return;
		}
		ComputeNode node = computeNodeRepository.findById(nodeId)
				.orElseThrow(() -> new BizException(404, "node not found"));
		int current = node.getRunningCount() == null ? 0 : node.getRunningCount();
		node.setRunningCount(Math.max(0, current + delta));
		computeNodeRepository.update(node);
	}

	public String getNodeToken(Long nodeId) {
		if (nodeId == null) {
			throw new BizException(400, "nodeId is required");
		}
		return computeNodeRepository.findById(nodeId)
				.map(ComputeNode::getNodeToken)
				.orElseThrow(() -> new BizException(404, "node not found"));
	}

	public boolean validateNodeToken(Long nodeId, String nodeToken) {
		if (nodeId == null || nodeToken == null || nodeToken.isBlank()) {
			return false;
		}
		return computeNodeRepository.findById(nodeId)
				.map(node -> nodeToken.equals(node.getNodeToken()))
				.orElse(false);
	}

	private NodeDetailResponse toNodeDetail(ComputeNode node) {
		NodeDetailResponse response = NodeAssembler.toDetailResponse(node);
		List<NodeSolverCapability> capabilities = nodeSolverCapabilityRepository.listByNodeId(node.getId());
		response.setSolverIds(capabilities.stream().filter(NodeSolverCapability::isEnabled).map(NodeSolverCapability::getSolverId).toList());
		return response;
	}

	private AvailableNodeResponse toAvailableNodeResponse(ComputeNode node) {
		AvailableNodeResponse response = new AvailableNodeResponse();
		response.setNodeId(node.getId());
		response.setNodeCode(node.getNodeCode());
		response.setNodeName(node.getNodeName());
		response.setHost(node.getHost());
		response.setRunningCount(node.getRunningCount());
		response.setMaxConcurrency(node.getMaxConcurrency());
		return response;
	}

	private String composeHost(String host) {
		if (host == null || host.isBlank()) {
			return host;
		}
		return host;
	}

	private ComputeNode resolveNode(NodeHeartbeatRequest request) {
		if (request.getNodeId() != null) {
			return computeNodeRepository.findById(request.getNodeId())
					.orElseThrow(() -> new BizException(404, "node not found"));
		}
		return computeNodeRepository.findByNodeCode(request.getNodeCode())
				.orElseThrow(() -> new BizException(404, "node not found"));
	}

	private String generateNodeToken(String nodeCode) {
		String source = (nodeCode == null ? "node" : nodeCode) + ":" + UUID.randomUUID();
		return Base64.getUrlEncoder().withoutPadding().encodeToString(source.getBytes(StandardCharsets.UTF_8));
	}

	private List<Long> extractSolverIds(NodeAgentRegisterRequest request) {
		if (request == null || request.getSolvers() == null || request.getSolvers().isEmpty()) {
			return List.of();
		}
		return request.getSolvers().stream()
				.map(NodeAgentRegisterRequest.SolverItem::getSolverId)
				.filter(id -> id != null)
				.toList();
	}

	private List<NodeSolverCapability> toCapabilities(Long nodeId, NodeAgentRegisterRequest request) {
		if (request == null || request.getSolvers() == null || request.getSolvers().isEmpty()) {
			return List.of();
		}
		return request.getSolvers().stream()
				.filter(item -> item != null && item.getSolverId() != null)
				.map(item -> {
					NodeSolverCapability capability = new NodeSolverCapability();
					capability.setNodeId(nodeId);
					capability.setSolverId(item.getSolverId());
					capability.setSolverVersion(item.getSolverVersion());
					capability.setEnabled(1);
					return capability;
				})
				.toList();
	}
}

