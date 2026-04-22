package com.example.cae.scheduler.application.service;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.exception.BizException;
import com.example.cae.common.response.PageResult;
import com.example.cae.scheduler.application.manager.NodeCapacityManager;
import com.example.cae.scheduler.application.assembler.NodeAssembler;
import com.example.cae.scheduler.domain.model.ComputeNode;
import com.example.cae.scheduler.domain.model.NodeSolverCapability;
import com.example.cae.scheduler.domain.repository.ComputeNodeRepository;
import com.example.cae.scheduler.domain.repository.NodeSolverCapabilityRepository;
import com.example.cae.scheduler.domain.service.NodeDomainService;
import com.example.cae.scheduler.infrastructure.client.SolverClient;
import com.example.cae.scheduler.interfaces.request.NodeAgentRegisterRequest;
import com.example.cae.scheduler.interfaces.request.NodeHeartbeatRequest;
import com.example.cae.scheduler.interfaces.request.NodePageQueryRequest;
import com.example.cae.scheduler.interfaces.request.NodeRegisterRequest;
import com.example.cae.scheduler.interfaces.request.UpdateNodeSolverStatusRequest;
import com.example.cae.scheduler.interfaces.request.UpdateNodeStatusRequest;
import com.example.cae.scheduler.interfaces.response.AvailableNodeResponse;
import com.example.cae.scheduler.interfaces.response.NodeDetailResponse;
import com.example.cae.scheduler.interfaces.response.NodeListItemResponse;
import com.example.cae.scheduler.interfaces.response.NodeReservationActionResponse;
import com.example.cae.scheduler.interfaces.response.NodeSolverResponse;
import com.example.cae.scheduler.interfaces.response.NodeSolverStatusResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class NodeAppService {
	private final ComputeNodeRepository computeNodeRepository;
	private final NodeSolverCapabilityRepository nodeSolverCapabilityRepository;
	private final NodeDomainService nodeDomainService;
	private final SolverClient solverClient;
	private final NodeCapacityManager nodeCapacityManager;

	public NodeAppService(ComputeNodeRepository computeNodeRepository,
						  NodeSolverCapabilityRepository nodeSolverCapabilityRepository,
						  NodeDomainService nodeDomainService,
						  SolverClient solverClient,
						  NodeCapacityManager nodeCapacityManager) {
		this.computeNodeRepository = computeNodeRepository;
		this.nodeSolverCapabilityRepository = nodeSolverCapabilityRepository;
		this.nodeDomainService = nodeDomainService;
		this.solverClient = solverClient;
		this.nodeCapacityManager = nodeCapacityManager;
	}

	public Long registerNode(NodeRegisterRequest request) {
		nodeDomainService.validateRegisterRequest(request);
		Optional<ComputeNode> existingNode = computeNodeRepository.findByNodeCode(request.getNodeCode());
		if (existingNode.isPresent()) {
			ComputeNode node = existingNode.get();
			node.setNodeName(request.getNodeName());
			node.setHost(request.getHost());
			node.setPort(request.getPort());
			node.setMaxConcurrency(request.getMaxConcurrency());
			if (node.getNodeToken() == null || node.getNodeToken().isBlank()) {
				node.setNodeToken(generateNodeToken(node.getNodeCode()));
			}
			node.markOnline();
			node.setLastHeartbeatTime(LocalDateTime.now());
			node.setReservedCount(node.getReservedCount() == null ? 0 : node.getReservedCount());
			computeNodeRepository.update(node);
			nodeSolverCapabilityRepository.replaceNodeCapabilitiesWithDetails(node.getId(),
					mergeCapabilities(node.getId(), request.getSolverIds(), null));
			return node.getId();
		}

		ComputeNode node = NodeAssembler.toNode(request);
		node.setNodeToken(generateNodeToken(node.getNodeCode()));
		node.markOnline();
		node.enable();
		node.setRunningCount(0);
		node.setReservedCount(0);
		node.setLastHeartbeatTime(LocalDateTime.now());
		computeNodeRepository.save(node);
		nodeSolverCapabilityRepository.replaceNodeCapabilitiesWithDetails(node.getId(),
				mergeCapabilities(node.getId(), request.getSolverIds(), null));
		return node.getId();
	}

	public Long registerNodeFromAgent(NodeAgentRegisterRequest request) {
		NodeRegisterRequest registerRequest = new NodeRegisterRequest();
		registerRequest.setNodeCode(request == null ? null : request.getNodeCode());
		registerRequest.setNodeName(request == null ? null : request.getNodeName());
		registerRequest.setHost(composeHost(request == null ? null : request.getHost()));
		registerRequest.setPort(request == null ? null : request.getPort());
		registerRequest.setMaxConcurrency(request == null ? null : request.getMaxConcurrency());
		registerRequest.setSolverIds(extractSolverIds(request));

		Long nodeId = registerNode(registerRequest);
		nodeSolverCapabilityRepository.replaceNodeCapabilitiesWithDetails(nodeId, mergeCapabilities(nodeId, null, request));
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
				throw new BizException(ErrorCodeConstants.NODE_TOKEN_REQUIRED, "node token required");
			}
			if (node.getNodeToken() == null || !nodeToken.equals(node.getNodeToken())) {
				throw new BizException(ErrorCodeConstants.INVALID_NODE_TOKEN, "invalid node token");
			}
		}
		node.refreshHeartbeat(request.getCpuUsage(), request.getMemoryUsage(), request.getRunningCount(), LocalDateTime.now());
		node.markOnline();
		computeNodeRepository.update(node);
	}

	public PageResult<NodeListItemResponse> pageNodes(NodePageQueryRequest request) {
		int pageNum = request == null || request.getPageNum() == null || request.getPageNum() < 1 ? 1 : request.getPageNum();
		int pageSize = request == null || request.getPageSize() == null || request.getPageSize() < 1 ? 10 : request.getPageSize();
		long offset = (long) (pageNum - 1) * pageSize;

		PageResult<ComputeNode> page = computeNodeRepository.page(request, offset, pageSize);
		List<NodeListItemResponse> records = page.getRecords().stream().map(this::toNodeListItem).toList();
		return PageResult.of(page.getTotal(), pageNum, pageSize, records);
	}

	public NodeDetailResponse getNodeDetail(Long nodeId) {
		ComputeNode node = computeNodeRepository.findById(nodeId).orElseThrow(() -> new BizException(ErrorCodeConstants.NODE_NOT_FOUND, "node not found"));
		return toNodeDetail(node);
	}

	public void updateNodeStatus(Long nodeId, UpdateNodeStatusRequest request) {
		if (nodeId == null || request == null || request.getEnabled() == null) {
			throw new BizException(ErrorCodeConstants.INVALID_NODE_STATUS_REQUEST, "invalid node status request");
		}
		ComputeNode node = computeNodeRepository.findById(nodeId).orElseThrow(() -> new BizException(ErrorCodeConstants.NODE_NOT_FOUND, "node not found"));
		node.setEnabled(request.getEnabled());
		computeNodeRepository.update(node);
	}

	public NodeSolverStatusResponse updateNodeSolverStatus(Long nodeId, Long solverId, UpdateNodeSolverStatusRequest request) {
		if (nodeId == null || solverId == null || request == null || request.getEnabled() == null) {
			throw new BizException(ErrorCodeConstants.INVALID_NODE_STATUS_REQUEST, "invalid node solver status request");
		}
		computeNodeRepository.findById(nodeId).orElseThrow(() -> new BizException(ErrorCodeConstants.NODE_NOT_FOUND, "node not found"));
		NodeSolverCapability capability = nodeSolverCapabilityRepository.listByNodeId(nodeId).stream()
				.filter(item -> solverId.equals(item.getSolverId()))
				.findFirst()
				.orElseThrow(() -> new BizException(ErrorCodeConstants.NOT_FOUND, "node solver capability not found"));
		capability.setEnabled(request.getEnabled());
		nodeSolverCapabilityRepository.update(capability);
		NodeSolverStatusResponse response = new NodeSolverStatusResponse();
		response.setNodeId(nodeId);
		response.setSolverId(solverId);
		response.setEnabled(capability.getEnabled());
		return response;
	}

	public List<NodeSolverResponse> listNodeSolvers(Long nodeId) {
		if (nodeId == null) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "nodeId is required");
		}
		computeNodeRepository.findById(nodeId).orElseThrow(() -> new BizException(ErrorCodeConstants.NODE_NOT_FOUND, "node not found"));
		return nodeSolverCapabilityRepository.listByNodeId(nodeId).stream()
				.map(this::toNodeSolverResponse)
				.toList();
	}

	public void markNodeOffline(String nodeCode) {
		computeNodeRepository.findByNodeCode(nodeCode).ifPresent(node -> {
			node.markOffline();
			node.setRunningCount(0);
			node.setCpuUsage(BigDecimal.ZERO);
			node.setMemoryUsage(BigDecimal.ZERO);
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

	@Transactional
	public NodeReservationActionResponse reserveReservation(Long nodeId, Long taskId) {
		return nodeCapacityManager.reserve(nodeId, taskId);
	}

	@Transactional
	public NodeReservationActionResponse releaseReservation(Long nodeId, Long taskId) {
		return nodeCapacityManager.release(nodeId, taskId);
	}

	public String getNodeToken(Long nodeId) {
		if (nodeId == null) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "nodeId is required");
		}
		return computeNodeRepository.findById(nodeId)
				.map(ComputeNode::getNodeToken)
				.orElseThrow(() -> new BizException(ErrorCodeConstants.NODE_NOT_FOUND, "node not found"));
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
		response.setSolvers(capabilities.stream().map(this::toNodeSolverResponse).toList());
		return response;
	}

	private NodeSolverResponse toNodeSolverResponse(NodeSolverCapability capability) {
		NodeSolverResponse response = new NodeSolverResponse();
		response.setSolverId(capability.getSolverId());
		response.setSolverVersion(capability.getSolverVersion());
		response.setEnabled(capability.getEnabled());
		if (capability.getSolverId() != null) {
			SolverClient.SolverMeta solverMeta = solverClient.getSolverMeta(capability.getSolverId());
			if (solverMeta != null) {
				response.setSolverName(solverMeta.getSolverName());
			}
		}
		return response;
	}

	private NodeListItemResponse toNodeListItem(ComputeNode node) {
		NodeListItemResponse response = new NodeListItemResponse();
		response.setNodeId(node.getId());
		response.setNodeCode(node.getNodeCode());
		response.setNodeName(node.getNodeName());
		response.setHost(node.getHost());
		response.setPort(node.getPort());
		response.setStatus(node.getStatus());
		response.setEnabled(node.getEnabled());
		response.setMaxConcurrency(node.getMaxConcurrency());
		response.setRunningCount(node.getRunningCount());
		response.setReservedCount(node.getReservedCount());
		response.setEffectiveLoad(node.getTotalLoad());
		response.setLoadRatio(resolveLoadRatio(node));
		response.setCpuUsage(node.getCpuUsage());
		response.setMemoryUsage(node.getMemoryUsage());
		response.setLastHeartbeatTime(node.getLastHeartbeatTime());
		return response;
	}

	private AvailableNodeResponse toAvailableNodeResponse(ComputeNode node) {
		AvailableNodeResponse response = new AvailableNodeResponse();
		response.setNodeId(node.getId());
		response.setNodeCode(node.getNodeCode());
		response.setNodeName(node.getNodeName());
		response.setHost(node.getHost());
		response.setPort(node.getPort());
		response.setRunningCount(node.getRunningCount());
		response.setReservedCount(node.getReservedCount());
		response.setMaxConcurrency(node.getMaxConcurrency());
		response.setEffectiveLoad(node.getTotalLoad());
		return response;
	}

	private BigDecimal resolveLoadRatio(ComputeNode node) {
		if (node == null || node.getMaxConcurrency() == null || node.getMaxConcurrency() <= 0) {
			return BigDecimal.ZERO;
		}
		return BigDecimal.valueOf(node.getTotalLoad())
				.divide(BigDecimal.valueOf(node.getMaxConcurrency()), 4, RoundingMode.HALF_UP);
	}

	private String composeHost(String host) {
		if (host == null || host.isBlank()) {
			return host;
		}
		return host;
	}

	private ComputeNode resolveNode(NodeHeartbeatRequest request) {
		return computeNodeRepository.findById(request.getNodeId())
				.orElseThrow(() -> new BizException(ErrorCodeConstants.NODE_NOT_FOUND, "node not found"));
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

	private List<NodeSolverCapability> mergeCapabilities(Long nodeId, List<Long> solverIds, NodeAgentRegisterRequest request) {
		List<NodeSolverCapability> existingCapabilities = nodeSolverCapabilityRepository.listByNodeId(nodeId);
		var existingBySolverId = existingCapabilities.stream()
				.filter(item -> item.getSolverId() != null)
				.collect(Collectors.toMap(NodeSolverCapability::getSolverId, Function.identity(), (left, right) -> left));

		if (request != null && request.getSolvers() != null && !request.getSolvers().isEmpty()) {
			return request.getSolvers().stream()
					.filter(item -> item != null && item.getSolverId() != null)
					.map(item -> {
						NodeSolverCapability existing = existingBySolverId.get(item.getSolverId());
						NodeSolverCapability capability = new NodeSolverCapability();
						capability.setNodeId(nodeId);
						capability.setSolverId(item.getSolverId());
						capability.setSolverVersion(item.getSolverVersion());
						capability.setEnabled(existing == null || existing.getEnabled() == null ? 1 : existing.getEnabled());
						return capability;
					})
					.sorted(Comparator.comparing(NodeSolverCapability::getSolverId))
					.toList();
		}

		if (solverIds == null || solverIds.isEmpty()) {
			return List.of();
		}
		return solverIds.stream()
				.filter(solverId -> solverId != null)
				.map(solverId -> {
					NodeSolverCapability existing = existingBySolverId.get(solverId);
					NodeSolverCapability capability = new NodeSolverCapability();
					capability.setNodeId(nodeId);
					capability.setSolverId(solverId);
					capability.setSolverVersion(existing == null ? null : existing.getSolverVersion());
					capability.setEnabled(existing == null || existing.getEnabled() == null ? 1 : existing.getEnabled());
					return capability;
				})
				.sorted(Comparator.comparing(NodeSolverCapability::getSolverId))
				.toList();
	}
}
