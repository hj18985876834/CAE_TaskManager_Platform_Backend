package com.example.cae.scheduler.application.service;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.dto.TaskDTO;
import com.example.cae.common.exception.BizException;
import com.example.cae.scheduler.domain.repository.ComputeNodeRepository;
import com.example.cae.scheduler.domain.repository.NodeSolverCapabilityRepository;
import com.example.cae.scheduler.domain.repository.ScheduleRecordRepository;
import com.example.cae.scheduler.domain.service.ScheduleDomainService;
import com.example.cae.scheduler.domain.strategy.ScheduleStrategy;
import com.example.cae.scheduler.infrastructure.client.NodeAgentClient;
import com.example.cae.scheduler.infrastructure.client.SolverClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
	private NodeAgentClient nodeAgentClient;
	@Mock
	private SolverClient solverClient;

	private ScheduleAppService scheduleAppService;

	@BeforeEach
	void setUp() {
		scheduleAppService = new ScheduleAppService(
				computeNodeRepository,
				nodeSolverCapabilityRepository,
				scheduleRecordRepository,
				scheduleDomainService,
				scheduleStrategy,
				nodeAgentClient,
				solverClient
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

	private TaskDTO buildTask() {
		TaskDTO task = new TaskDTO();
		task.setTaskId(1001L);
		task.setSolverId(11L);
		task.setProfileId(21L);
		return task;
	}
}
