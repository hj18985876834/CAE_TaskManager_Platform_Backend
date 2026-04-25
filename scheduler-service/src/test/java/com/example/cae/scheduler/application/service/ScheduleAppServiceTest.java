package com.example.cae.scheduler.application.service;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.dto.TaskDTO;
import com.example.cae.common.exception.BizException;
import com.example.cae.scheduler.domain.model.ComputeNode;
import com.example.cae.scheduler.domain.model.NodeSolverCapability;
import com.example.cae.scheduler.domain.repository.ComputeNodeRepository;
import com.example.cae.scheduler.domain.repository.NodeSolverCapabilityRepository;
import com.example.cae.scheduler.domain.repository.ScheduleRecordRepository;
import com.example.cae.scheduler.domain.service.ScheduleDomainService;
import com.example.cae.scheduler.domain.strategy.ScheduleStrategy;
import com.example.cae.scheduler.application.manager.NodeCapacityManager;
import com.example.cae.scheduler.infrastructure.client.SolverClient;
import com.example.cae.scheduler.infrastructure.client.TaskClient;
import com.example.cae.scheduler.interfaces.response.NodeReservationActionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleAppServiceTest {
	@Mock
	private ComputeNodeRepository computeNodeRepository;
	@Mock
	private NodeSolverCapabilityRepository nodeSolverCapabilityRepository;
	@Mock
	private ScheduleRecordRepository scheduleRecordRepository;
	@Mock
	private ScheduleDomainService scheduleDomainService;
	@Mock
	private ScheduleStrategy scheduleStrategy;
	@Mock
	private TaskClient taskClient;
	@Mock
	private SolverClient solverClient;
	@Mock
	private NodeCapacityManager nodeCapacityManager;

	private ScheduleAppService scheduleAppService;

	@BeforeEach
	void setUp() {
		scheduleAppService = new ScheduleAppService(
				computeNodeRepository,
				nodeSolverCapabilityRepository,
				scheduleRecordRepository,
				scheduleDomainService,
				scheduleStrategy,
				solverClient,
				taskClient,
				nodeCapacityManager
		);
	}

	@Test
	void scheduleTaskShouldRejectDisabledSolver() {
		TaskDTO task = buildTask();
		SolverClient.SolverMeta solverMeta = new SolverClient.SolverMeta();
		solverMeta.setSolverId(task.getSolverId());
		solverMeta.setSolverName("OptiStruct");
		solverMeta.setEnabled(0);
		when(solverClient.getSolverMeta(task.getSolverId())).thenReturn(solverMeta);

		BizException exception = assertThrows(BizException.class, () -> scheduleAppService.scheduleTask(task));

		assertEquals(ErrorCodeConstants.SOLVER_DISABLED, exception.getCode());
		verifyNoInteractions(computeNodeRepository, nodeSolverCapabilityRepository, scheduleDomainService, scheduleStrategy);
	}

	@Test
	void scheduleTaskShouldRejectDisabledProfile() {
		TaskDTO task = buildTask();
		SolverClient.SolverMeta solverMeta = new SolverClient.SolverMeta();
		solverMeta.setSolverId(task.getSolverId());
		solverMeta.setEnabled(1);
		SolverClient.ProfileMeta profileMeta = new SolverClient.ProfileMeta();
		profileMeta.setProfileId(task.getProfileId());
		profileMeta.setProfileName("Static Analysis");
		profileMeta.setEnabled(0);
		when(solverClient.getSolverMeta(task.getSolverId())).thenReturn(solverMeta);
		when(solverClient.getProfileMeta(task.getProfileId())).thenReturn(profileMeta);

		BizException exception = assertThrows(BizException.class, () -> scheduleAppService.scheduleTask(task));

		assertEquals(ErrorCodeConstants.PROFILE_DISABLED, exception.getCode());
		verifyNoInteractions(computeNodeRepository, nodeSolverCapabilityRepository, scheduleDomainService, scheduleStrategy);
	}

	@Test
	void recordScheduleFailureShouldAllowNullNodeIdWhenNoNodeSelected() {
		scheduleAppService.recordScheduleFailure(1001L, null, "no available node");

		ArgumentCaptor<com.example.cae.scheduler.domain.model.ScheduleRecord> captor =
				ArgumentCaptor.forClass(com.example.cae.scheduler.domain.model.ScheduleRecord.class);
		verify(scheduleRecordRepository).save(captor.capture());
		assertEquals(1001L, captor.getValue().getTaskId());
		assertNull(captor.getValue().getNodeId());
		assertEquals("FAILED", captor.getValue().getScheduleStatus());
	}

	@Test
	void scheduleTaskShouldReserveSelectedNodeSlot() {
		TaskDTO task = buildTask();
		ComputeNode selected = buildNode(31L, 4, 1);
		NodeSolverCapability capability = buildCapability(task.getSolverId(), selected.getId());
		NodeReservationActionResponse reservation = new NodeReservationActionResponse();
		reservation.setNodeId(selected.getId());
		reservation.setTaskId(task.getTaskId());
		reservation.setReservationStatus("RESERVED");
		reservation.setReservedCount(1);
		stubEnabledSolverAndProfile(task);
		when(computeNodeRepository.listByStatus("ONLINE")).thenReturn(List.of(selected));
		when(nodeSolverCapabilityRepository.listBySolverId(task.getSolverId())).thenReturn(List.of(capability));
		when(scheduleDomainService.filterAvailableNodes(anyList(), eq(task.getSolverId()), anyList())).thenReturn(List.of(selected));
		when(scheduleStrategy.orderNodes(eq(task), anyList())).thenReturn(List.of(selected));
		when(nodeCapacityManager.reserve(eq(selected.getId()), eq(task.getTaskId()))).thenReturn(reservation);

		Long nodeId = scheduleAppService.scheduleTask(task);

		assertEquals(selected.getId(), nodeId);
		verify(nodeCapacityManager).reserve(selected.getId(), task.getTaskId());
	}

	@Test
	void scheduleTaskShouldRejectWhenLockedNodeHasNoCapacity() {
		TaskDTO task = buildTask();
		ComputeNode selected = buildNode(31L, 1, 0);
		NodeSolverCapability capability = buildCapability(task.getSolverId(), selected.getId());
		stubEnabledSolverAndProfile(task);
		when(computeNodeRepository.listByStatus("ONLINE")).thenReturn(List.of(selected));
		when(nodeSolverCapabilityRepository.listBySolverId(task.getSolverId())).thenReturn(List.of(capability));
		when(scheduleDomainService.filterAvailableNodes(anyList(), eq(task.getSolverId()), anyList())).thenReturn(List.of(selected));
		when(scheduleStrategy.orderNodes(eq(task), anyList())).thenReturn(List.of(selected));
		when(nodeCapacityManager.reserve(eq(selected.getId()), eq(task.getTaskId())))
				.thenThrow(new BizException(ErrorCodeConstants.NO_AVAILABLE_NODE, "no available node"));

		BizException exception = assertThrows(BizException.class, () -> scheduleAppService.scheduleTask(task));

		assertEquals(ErrorCodeConstants.NO_AVAILABLE_NODE, exception.getCode());
	}

	@Test
	void handleDispatchFailureShouldRecordFormalRejectedRunningAudit() {
		when(taskClient.markTaskFailed(
				1001L,
				31L,
				"DISPATCH_ERROR",
				DispatchFailureMessageConstants.DISPATCH_WATCHDOG_TIMEOUT_RUNTIME_MISSING,
				true
		))
				.thenThrow(new BizException(
						ErrorCodeConstants.TASK_STATUS_TRANSFER_ILLEGAL,
						"dispatch-failed is not allowed after task entered RUNNING"
				));

		assertThrows(
				BizException.class,
				() -> scheduleAppService.handleDispatchFailure(
						1001L,
						31L,
						"DISPATCH_ERROR",
						DispatchFailureMessageConstants.DISPATCH_WATCHDOG_TIMEOUT_RUNTIME_MISSING,
						true
				)
		);

		ArgumentCaptor<com.example.cae.scheduler.domain.model.ScheduleRecord> captor =
				ArgumentCaptor.forClass(com.example.cae.scheduler.domain.model.ScheduleRecord.class);
		verify(scheduleRecordRepository).save(captor.capture());
		assertEquals(DispatchFailureMessageConstants.DISPATCH_FAILED_REJECTED_RUNNING, captor.getValue().getScheduleMessage());
	}

	@Test
	void handleDispatchFailureShouldRecordWatchdogReasonAfterSuccessfulRecovery() {
		com.example.cae.common.dto.TaskStatusAckDTO ack = new com.example.cae.common.dto.TaskStatusAckDTO();
		ack.setTaskId(1001L);
		ack.setStatus("QUEUED");
		when(taskClient.markTaskFailed(1001L, 31L, "DISPATCH_ERROR", DispatchFailureMessageConstants.DISPATCH_WATCHDOG_TIMEOUT_RUNTIME_MISSING, true))
				.thenReturn(ack);
		NodeReservationActionResponse release = new NodeReservationActionResponse();
		release.setTaskId(1001L);
		release.setNodeId(31L);
		release.setReservationStatus("RELEASED");
		release.setReservedCount(0);
		when(nodeCapacityManager.release(31L, 1001L)).thenReturn(release);

		scheduleAppService.handleDispatchFailure(
				1001L,
				31L,
				"DISPATCH_ERROR",
				DispatchFailureMessageConstants.DISPATCH_WATCHDOG_TIMEOUT_RUNTIME_MISSING,
				true
		);

		ArgumentCaptor<com.example.cae.scheduler.domain.model.ScheduleRecord> captor =
				ArgumentCaptor.forClass(com.example.cae.scheduler.domain.model.ScheduleRecord.class);
		verify(scheduleRecordRepository).save(captor.capture());
		assertEquals("FAILED", captor.getValue().getScheduleStatus());
		assertEquals(DispatchFailureMessageConstants.DISPATCH_WATCHDOG_TIMEOUT_RUNTIME_MISSING, captor.getValue().getScheduleMessage());
	}

	@Test
	void handleDispatchFailureShouldRecordReleaseFailureAuditBeforeThrowing() {
		com.example.cae.common.dto.TaskStatusAckDTO ack = new com.example.cae.common.dto.TaskStatusAckDTO();
		ack.setTaskId(1001L);
		ack.setStatus("QUEUED");
		when(taskClient.markTaskFailed(1001L, 31L, "DISPATCH_ERROR", DispatchFailureMessageConstants.DISPATCH_WATCHDOG_TIMEOUT_RUNTIME_MISSING, true))
				.thenReturn(ack);
		when(nodeCapacityManager.release(31L, 1001L))
				.thenThrow(new BizException(ErrorCodeConstants.BAD_GATEWAY, "scheduler release failed"));

		DispatchFailureReleaseException exception = assertThrows(
				DispatchFailureReleaseException.class,
				() -> scheduleAppService.handleDispatchFailure(
						1001L,
						31L,
						"DISPATCH_ERROR",
						DispatchFailureMessageConstants.DISPATCH_WATCHDOG_TIMEOUT_RUNTIME_MISSING,
						true
				)
		);

		ArgumentCaptor<com.example.cae.scheduler.domain.model.ScheduleRecord> captor =
				ArgumentCaptor.forClass(com.example.cae.scheduler.domain.model.ScheduleRecord.class);
		verify(scheduleRecordRepository).save(captor.capture());
		assertEquals("FAILED", captor.getValue().getScheduleStatus());
		assertTrue(captor.getValue().getScheduleMessage().startsWith(DispatchFailureMessageConstants.DISPATCH_FAILURE_RELEASE_FAILED_PREFIX));
		assertTrue(exception.getMessage().startsWith(DispatchFailureMessageConstants.DISPATCH_FAILURE_RELEASE_FAILED_PREFIX));
	}

	private TaskDTO buildTask() {
		TaskDTO task = new TaskDTO();
		task.setTaskId(1001L);
		task.setSolverId(11L);
		task.setProfileId(21L);
		return task;
	}

	private void stubEnabledSolverAndProfile(TaskDTO task) {
		SolverClient.SolverMeta solverMeta = new SolverClient.SolverMeta();
		solverMeta.setSolverId(task.getSolverId());
		solverMeta.setEnabled(1);
		SolverClient.ProfileMeta profileMeta = new SolverClient.ProfileMeta();
		profileMeta.setProfileId(task.getProfileId());
		profileMeta.setEnabled(1);
		profileMeta.setSolverId(task.getSolverId());
		when(solverClient.getSolverMeta(task.getSolverId())).thenReturn(solverMeta);
		when(solverClient.getProfileMeta(task.getProfileId())).thenReturn(profileMeta);
	}

	private ComputeNode buildNode(Long nodeId, int maxConcurrency, int totalLoad) {
		ComputeNode node = new ComputeNode();
		node.setId(nodeId);
		node.setNodeCode("node-31");
		node.setStatus("ONLINE");
		node.setEnabled(1);
		node.setMaxConcurrency(maxConcurrency);
		node.setRunningCount(totalLoad);
		node.setReservedCount(0);
		return node;
	}

	private NodeSolverCapability buildCapability(Long solverId, Long nodeId) {
		NodeSolverCapability capability = new NodeSolverCapability();
		capability.setSolverId(solverId);
		capability.setNodeId(nodeId);
		capability.setEnabled(1);
		return capability;
	}
}
