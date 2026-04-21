package com.example.cae.scheduler.application.service;

import com.example.cae.scheduler.domain.model.ComputeNode;
import com.example.cae.scheduler.domain.model.NodeSolverCapability;
import com.example.cae.scheduler.domain.repository.ComputeNodeRepository;
import com.example.cae.scheduler.domain.repository.NodeSolverCapabilityRepository;
import com.example.cae.scheduler.domain.service.NodeDomainService;
import com.example.cae.scheduler.infrastructure.client.SolverClient;
import com.example.cae.scheduler.interfaces.request.NodeAgentRegisterRequest;
import com.example.cae.scheduler.interfaces.request.NodeHeartbeatRequest;
import com.example.cae.scheduler.interfaces.response.NodeDetailResponse;
import com.example.cae.scheduler.interfaces.response.NodeSolverResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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

	@Test
	void heartbeatShouldUpdateRunningCountButKeepReservedCount() {
		ComputeNode node = buildNode();
		node.setReservedCount(2);
		when(computeNodeRepository.findById(1L)).thenReturn(Optional.of(node));

		NodeHeartbeatRequest request = new NodeHeartbeatRequest();
		request.setNodeId(1L);
		request.setCpuUsage(java.math.BigDecimal.valueOf(21.5));
		request.setMemoryUsage(java.math.BigDecimal.valueOf(35.2));
		request.setRunningCount(1);

		nodeAppService.heartbeat(request);

		ArgumentCaptor<ComputeNode> captor = ArgumentCaptor.forClass(ComputeNode.class);
		verify(computeNodeRepository).update(captor.capture());
		assertEquals(1, captor.getValue().getRunningCount());
		assertEquals(2, captor.getValue().getReservedCount());
		assertEquals("ONLINE", captor.getValue().getStatus());
	}

	@Test
	void markNodeOfflineShouldClearRunningAndReservedCount() {
		ComputeNode node = buildNode();
		node.setRunningCount(1);
		node.setReservedCount(2);
		when(computeNodeRepository.findByNodeCode("node-01")).thenReturn(Optional.of(node));

		nodeAppService.markNodeOffline("node-01");

		ArgumentCaptor<ComputeNode> captor = ArgumentCaptor.forClass(ComputeNode.class);
		verify(computeNodeRepository).update(captor.capture());
		assertEquals("OFFLINE", captor.getValue().getStatus());
		assertEquals(0, captor.getValue().getRunningCount());
		assertEquals(0, captor.getValue().getReservedCount());
		assertTrue(java.math.BigDecimal.ZERO.compareTo(captor.getValue().getCpuUsage()) == 0);
		assertTrue(java.math.BigDecimal.ZERO.compareTo(captor.getValue().getMemoryUsage()) == 0);
	}

	@Test
	void updateRunningCountShouldRemainCompatibilityNoOp() {
		nodeAppService.updateRunningCount(1L, 1);

		verify(computeNodeRepository, never()).update(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void registerNodeFromAgentShouldPreserveAdminCapabilitySwitch() {
		ComputeNode node = buildNode();
		NodeSolverCapability existingCapability = buildCapability();
		existingCapability.setEnabled(0);
		when(computeNodeRepository.findByNodeCode("node-01")).thenReturn(Optional.of(node));
		when(nodeSolverCapabilityRepository.listByNodeId(1L)).thenReturn(List.of(existingCapability));

		NodeAgentRegisterRequest request = new NodeAgentRegisterRequest();
		request.setNodeCode("node-01");
		request.setNodeName("Node 01");
		request.setHost("127.0.0.1");
		request.setMaxConcurrency(2);
		NodeAgentRegisterRequest.SolverItem solverItem = new NodeAgentRegisterRequest.SolverItem();
		solverItem.setSolverId(200L);
		solverItem.setSolverVersion("2026.1");
		request.setSolvers(List.of(solverItem));

		nodeAppService.registerNodeFromAgent(request);

		ArgumentCaptor<List<NodeSolverCapability>> captor = ArgumentCaptor.forClass(List.class);
		verify(nodeSolverCapabilityRepository, times(2)).replaceNodeCapabilitiesWithDetails(org.mockito.ArgumentMatchers.eq(1L), captor.capture());
		List<NodeSolverCapability> finalCapabilities = captor.getAllValues().get(captor.getAllValues().size() - 1);
		assertEquals(1, finalCapabilities.size());
		assertEquals(0, finalCapabilities.get(0).getEnabled());
		assertEquals("2026.1", finalCapabilities.get(0).getSolverVersion());
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
