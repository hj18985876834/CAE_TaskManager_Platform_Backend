package com.example.cae.scheduler.domain.repository;

import com.example.cae.scheduler.domain.model.NodeSolverCapability;

import java.util.List;

public interface NodeSolverCapabilityRepository {
	List<NodeSolverCapability> listByNodeId(Long nodeId);

	List<NodeSolverCapability> listBySolverId(Long solverId);

	void replaceNodeCapabilities(Long nodeId, List<Long> solverIds);

	void replaceNodeCapabilitiesWithDetails(Long nodeId, List<NodeSolverCapability> capabilities);
}
