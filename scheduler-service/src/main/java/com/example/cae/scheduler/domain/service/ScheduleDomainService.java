package com.example.cae.scheduler.domain.service;

import com.example.cae.scheduler.domain.model.ComputeNode;
import com.example.cae.scheduler.domain.model.NodeSolverCapability;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ScheduleDomainService {
	private final NodeDomainService nodeDomainService;

	public ScheduleDomainService(NodeDomainService nodeDomainService) {
		this.nodeDomainService = nodeDomainService;
	}

	public List<ComputeNode> filterAvailableNodes(List<ComputeNode> nodes, Long solverId, List<NodeSolverCapability> capabilities) {
		Set<Long> supportedNodeIds = capabilities.stream()
				.filter(NodeSolverCapability::isEnabled)
				.filter(item -> Objects.equals(item.getSolverId(), solverId))
				.map(NodeSolverCapability::getNodeId)
				.collect(Collectors.toSet());

		return nodes.stream()
				.filter(nodeDomainService::canDispatch)
				.filter(node -> supportedNodeIds.contains(node.getId()))
				.toList();
	}
}

