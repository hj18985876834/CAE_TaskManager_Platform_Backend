package com.example.cae.scheduler.infrastructure.support;

import com.example.cae.common.dto.TaskBasicDTO;
import com.example.cae.common.enums.FailTypeEnum;
import com.example.cae.scheduler.application.manager.TaskScheduleManager;
import com.example.cae.scheduler.application.service.DispatchFailureMessageConstants;
import com.example.cae.scheduler.application.service.NodeAppService;
import com.example.cae.scheduler.domain.model.ComputeNode;
import com.example.cae.scheduler.domain.model.NodeReservation;
import com.example.cae.scheduler.domain.repository.NodeReservationRepository;
import com.example.cae.scheduler.infrastructure.client.NodeAgentClient;
import com.example.cae.scheduler.infrastructure.client.TaskClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DispatchStallCheckerTest {
	@Mock
	private NodeAppService nodeAppService;
	@Mock
	private NodeReservationRepository nodeReservationRepository;
	@Mock
	private TaskClient taskClient;
	@Mock
	private NodeAgentClient nodeAgentClient;
	@Mock
	private TaskScheduleManager taskScheduleManager;

	private DispatchStallChecker dispatchStallChecker;

	@BeforeEach
	void setUp() {
		dispatchStallChecker = new DispatchStallChecker(
				nodeAppService,
				nodeReservationRepository,
				taskClient,
				nodeAgentClient,
				taskScheduleManager,
				30L
		);
	}

	@Test
	void recoverStalledDispatchesShouldRequeueSameNodeScheduledTaskWhenRuntimeMissing() {
		ComputeNode node = onlineNode(21L, 5);
		NodeReservation reservation = reserved(21L, 1001L, 45);
		TaskBasicDTO taskBasic = taskBasic(1001L, 21L, "SCHEDULED");
		when(nodeAppService.listOnlineNodes()).thenReturn(List.of(node));
		when(nodeReservationRepository.listReservedByNodeId(21L)).thenReturn(List.of(reservation));
		when(taskClient.getTaskBasics(List.of(1001L))).thenReturn(Map.of(1001L, taskBasic));
		when(nodeAgentClient.isTaskActive(21L, 1001L)).thenReturn(false);

		dispatchStallChecker.recoverStalledDispatches();

		verify(taskScheduleManager).handleDispatchFailure(
				1001L,
				21L,
				FailTypeEnum.DISPATCH_ERROR.name(),
				"dispatch watchdog timeout, node-agent runtime missing",
				true
		);
	}

	@Test
	void watchdogReasonShouldStayOnFormalAuditContract() {
		assertEquals(
				"dispatch watchdog timeout, node-agent runtime missing",
				DispatchFailureMessageConstants.DISPATCH_WATCHDOG_TIMEOUT_RUNTIME_MISSING
		);
	}

	@Test
	void recoverStalledDispatchesShouldSkipWhenRuntimeStillActive() {
		ComputeNode node = onlineNode(21L, 5);
		NodeReservation reservation = reserved(21L, 1001L, 45);
		TaskBasicDTO taskBasic = taskBasic(1001L, 21L, "DISPATCHED");
		when(nodeAppService.listOnlineNodes()).thenReturn(List.of(node));
		when(nodeReservationRepository.listReservedByNodeId(21L)).thenReturn(List.of(reservation));
		when(taskClient.getTaskBasics(List.of(1001L))).thenReturn(Map.of(1001L, taskBasic));
		when(nodeAgentClient.isTaskActive(21L, 1001L)).thenReturn(true);

		dispatchStallChecker.recoverStalledDispatches();

		verify(taskScheduleManager, never()).handleDispatchFailure(
				1001L,
				21L,
				FailTypeEnum.DISPATCH_ERROR.name(),
				"dispatch watchdog timeout, node-agent runtime missing",
				true
		);
	}

	@Test
	void recoverStalledDispatchesShouldSkipFreshReservationOrMismatchedState() {
		ComputeNode node = onlineNode(21L, 5);
		NodeReservation freshReservation = reserved(21L, 1001L, 5);
		NodeReservation staleReservation = reserved(21L, 1002L, 45);
		TaskBasicDTO runningTask = taskBasic(1002L, 21L, "RUNNING");
		when(nodeAppService.listOnlineNodes()).thenReturn(List.of(node));
		when(nodeReservationRepository.listReservedByNodeId(21L)).thenReturn(List.of(freshReservation, staleReservation));
		when(taskClient.getTaskBasics(List.of(1002L))).thenReturn(Map.of(1002L, runningTask));

		dispatchStallChecker.recoverStalledDispatches();

		verify(nodeAgentClient, never()).isTaskActive(21L, 1001L);
		verify(nodeAgentClient, never()).isTaskActive(21L, 1002L);
		verify(taskScheduleManager, never()).handleDispatchFailure(
				1002L,
				21L,
				FailTypeEnum.DISPATCH_ERROR.name(),
				"dispatch watchdog timeout, node-agent runtime missing",
				true
		);
	}

	private ComputeNode onlineNode(Long nodeId, long heartbeatSecondsAgo) {
		ComputeNode node = new ComputeNode();
		node.setId(nodeId);
		node.setStatus("ONLINE");
		node.setLastHeartbeatTime(LocalDateTime.now().minusSeconds(heartbeatSecondsAgo));
		return node;
	}

	private NodeReservation reserved(Long nodeId, Long taskId, long secondsAgo) {
		NodeReservation reservation = new NodeReservation();
		reservation.setNodeId(nodeId);
		reservation.setTaskId(taskId);
		reservation.setStatus("RESERVED");
		reservation.setCreatedAt(LocalDateTime.now().minusSeconds(secondsAgo));
		reservation.setUpdatedAt(LocalDateTime.now().minusSeconds(secondsAgo));
		return reservation;
	}

	private TaskBasicDTO taskBasic(Long taskId, Long nodeId, String status) {
		TaskBasicDTO taskBasic = new TaskBasicDTO();
		taskBasic.setTaskId(taskId);
		taskBasic.setNodeId(nodeId);
		taskBasic.setStatus(status);
		return taskBasic;
	}
}
