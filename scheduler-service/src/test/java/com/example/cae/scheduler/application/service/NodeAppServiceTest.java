package com.example.cae.scheduler.application.service;

import com.example.cae.scheduler.domain.model.ComputeNode;
import com.example.cae.scheduler.domain.model.NodeSolverCapability;
import com.example.cae.scheduler.domain.repository.ComputeNodeRepository;
import com.example.cae.scheduler.domain.repository.NodeSolverCapabilityRepository;
import com.example.cae.scheduler.domain.service.NodeDomainService;
import com.example.cae.scheduler.infrastructure.client.SolverClient;
import com.example.cae.scheduler.interfaces.response.NodeDetailResponse;
import com.example.cae.scheduler.interfaces.response.NodeSolverResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NodeAppServiceTest {
	@Mock
	private ComputeNodeRepository computeNodeRepository;
	@Mock
	private NodeSolverCapabilityRepository nodeSolverCapabilityRepository;
	@Mock
	private NodeDomainService nodeDomainService;
	@Mock
	private SolverClient solverClient;

	private NodeAppService nodeAppService;

	@BeforeEach
	void setUp() {
		nodeAppService = new NodeAppService(
				computeNodeRepository,
				nodeSolverCapabilityRepository,
				nodeDomainService,
				solverClient
		);
	}

	@Test
	void listNodeSolversShouldIncludeSolverName() {
		ComputeNode node = buildNode();
		NodeSolverCapability capability = buildCapability();
		SolverClient.SolverMeta solverMeta = new SolverClient.SolverMeta();
		solverMeta.setSolverId(200L);
		solverMeta.setSolverName("CalculiX");
		when(computeNodeRepository.findById(1L)).thenReturn(Optional.of(node));
		when(nodeSolverCapabilityRepository.listByNodeId(1L)).thenReturn(List.of(capability));
		when(solverClient.getSolverMeta(200L)).thenReturn(solverMeta);

		List<NodeSolverResponse> responses = nodeAppService.listNodeSolvers(1L);

		assertEquals(1, responses.size());
		assertEquals("CalculiX", responses.get(0).getSolverName());
	}

	@Test
	void getNodeDetailShouldIncludeSolverName() {
		ComputeNode node = buildNode();
		NodeSolverCapability capability = buildCapability();
		SolverClient.SolverMeta solverMeta = new SolverClient.SolverMeta();
		solverMeta.setSolverId(200L);
		solverMeta.setSolverName("CalculiX");
		when(computeNodeRepository.findById(1L)).thenReturn(Optional.of(node));
		when(nodeSolverCapabilityRepository.listByNodeId(1L)).thenReturn(List.of(capability));
		when(solverClient.getSolverMeta(200L)).thenReturn(solverMeta);

		NodeDetailResponse response = nodeAppService.getNodeDetail(1L);

		assertEquals(1, response.getSolvers().size());
		assertEquals("CalculiX", response.getSolvers().get(0).getSolverName());
	}

	private ComputeNode buildNode() {
		ComputeNode node = new ComputeNode();
		node.setId(1L);
		node.setNodeCode("node-01");
		node.setNodeName("Node 01");
		node.setHost("127.0.0.1:9001");
		node.setStatus("ONLINE");
		node.setEnabled(1);
		node.setMaxConcurrency(2);
		node.setRunningCount(0);
		node.setReservedCount(0);
		return node;
	}

	private NodeSolverCapability buildCapability() {
		NodeSolverCapability capability = new NodeSolverCapability();
		capability.setNodeId(1L);
		capability.setSolverId(200L);
		capability.setSolverVersion("2026.1");
		capability.setEnabled(1);
		return capability;
	}
}
