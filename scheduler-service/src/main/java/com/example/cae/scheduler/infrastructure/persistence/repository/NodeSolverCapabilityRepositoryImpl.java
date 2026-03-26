package com.example.cae.scheduler.infrastructure.persistence.repository;

import com.example.cae.scheduler.domain.model.NodeSolverCapability;
import com.example.cae.scheduler.domain.repository.NodeSolverCapabilityRepository;
import com.example.cae.scheduler.infrastructure.persistence.entity.NodeSolverCapabilityPO;
import com.example.cae.scheduler.infrastructure.persistence.mapper.NodeSolverCapabilityMapper;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
public class NodeSolverCapabilityRepositoryImpl implements NodeSolverCapabilityRepository {
	private final NodeSolverCapabilityMapper nodeSolverCapabilityMapper;

	public NodeSolverCapabilityRepositoryImpl(NodeSolverCapabilityMapper nodeSolverCapabilityMapper) {
		this.nodeSolverCapabilityMapper = nodeSolverCapabilityMapper;
	}

	@Override
	public List<NodeSolverCapability> listByNodeId(Long nodeId) {
		return nodeSolverCapabilityMapper.selectByNodeId(nodeId).stream().map(this::fromPO).toList();
	}

	@Override
	public List<NodeSolverCapability> listBySolverId(Long solverId) {
		return nodeSolverCapabilityMapper.selectBySolverId(solverId).stream().map(this::fromPO).toList();
	}

	@Override
	public void replaceNodeCapabilities(Long nodeId, List<Long> solverIds) {
		nodeSolverCapabilityMapper.deleteByNodeId(nodeId);
		if (solverIds == null || solverIds.isEmpty()) {
			return;
		}
		nodeSolverCapabilityMapper.batchInsert(nodeId, solverIds);
	}

	private NodeSolverCapability fromPO(NodeSolverCapabilityPO po) {
		NodeSolverCapability capability = new NodeSolverCapability();
		capability.setId(po.getId());
		capability.setNodeId(po.getNodeId());
		capability.setSolverId(po.getSolverId());
		capability.setSolverVersion(po.getSolverVersion());
		capability.setEnabled(po.getEnabled());
		capability.setCreatedAt(po.getCreatedAt());
		return capability;
	}
}
