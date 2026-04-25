package com.example.cae.task.application.manager;

import com.example.cae.common.enums.TaskStatusEnum;
import com.example.cae.common.dto.TaskDispatchAckDTO;
import com.example.cae.common.dto.TaskScheduleClaimDTO;
import com.example.cae.task.domain.model.Task;
import com.example.cae.task.domain.model.TaskStatusHistory;
import com.example.cae.task.domain.repository.TaskFileRepository;
import com.example.cae.task.domain.repository.TaskRepository;
import com.example.cae.task.domain.repository.TaskResultFileRepository;
import com.example.cae.task.domain.repository.TaskResultSummaryRepository;
import com.example.cae.task.domain.repository.TaskStatusHistoryRepository;
import com.example.cae.task.domain.rule.TaskStatusRule;
import com.example.cae.task.domain.service.TaskStatusDomainService;
import com.example.cae.task.application.support.TaskStatusHistoryMessageConstants;
import com.example.cae.task.infrastructure.client.SchedulerClient;
import com.example.cae.task.infrastructure.client.SolverClient;
import com.example.cae.task.infrastructure.support.TaskStoragePathSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskDispatchManagerTest {
	@Mock
	private TaskRepository taskRepository;
	@Mock
	private TaskFileRepository taskFileRepository;
	@Mock
	private TaskStatusHistoryRepository taskStatusHistoryRepository;
	@Mock
	private TaskResultSummaryRepository taskResultSummaryRepository;
	@Mock
	private TaskResultFileRepository taskResultFileRepository;
	@Mock
	private SchedulerClient schedulerClient;
	@Mock
	private SolverClient solverClient;
	@Mock
	private TaskStoragePathSupport taskStoragePathSupport;
	@Mock
	private PlatformTransactionManager transactionManager;

	private TaskDispatchManager taskDispatchManager;

	@BeforeEach
	void setUp() {
		TaskStatusDomainService taskStatusDomainService = new TaskStatusDomainService(
				new TaskStatusRule(),
				taskStatusHistoryRepository
		);
		taskDispatchManager = new TaskDispatchManager(
				taskRepository,
				taskFileRepository,
				taskResultSummaryRepository,
				taskResultFileRepository,
				taskStatusHistoryRepository,
				taskStatusDomainService,
				schedulerClient,
				solverClient,
				taskStoragePathSupport,
				transactionManager
		);
	}

	@Test
	void markScheduledShouldBindNodeOnlyWhenTaskStillQueued() {
		Task task = buildTask(TaskStatusEnum.QUEUED.name());
		when(taskRepository.findByIdForUpdate(1001L)).thenReturn(Optional.of(task));

		TaskScheduleClaimDTO claim = taskDispatchManager.markScheduled(1001L, 21L);

		assertTrue(Boolean.TRUE.equals(claim.getClaimed()));
		assertEquals(TaskStatusEnum.SCHEDULED.name(), claim.getStatus());
		assertEquals(21L, claim.getNodeId());
		ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
		verify(taskRepository).update(captor.capture());
		assertEquals(TaskStatusEnum.SCHEDULED.name(), captor.getValue().getStatus());
		assertEquals(21L, captor.getValue().getNodeId());
		verify(taskStatusHistoryRepository).save(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void markScheduledShouldReturnFalseWhenTaskAlreadyAdvanced() {
		Task task = buildTask(TaskStatusEnum.RUNNING.name());
		when(taskRepository.findByIdForUpdate(1001L)).thenReturn(Optional.of(task));

		TaskScheduleClaimDTO claim = taskDispatchManager.markScheduled(1001L, 21L);

		assertFalse(Boolean.TRUE.equals(claim.getClaimed()));
		assertEquals(TaskStatusEnum.RUNNING.name(), claim.getStatus());
		assertEquals(21L, claim.getNodeId());
		verify(taskRepository, never()).update(org.mockito.ArgumentMatchers.any());
		verifyNoInteractions(taskStatusHistoryRepository);
	}

	@Test
	void markDispatchedShouldOnlyAdvanceScheduledTask() {
		Task task = buildTask(TaskStatusEnum.SCHEDULED.name());
		task.setNodeId(21L);
		when(taskRepository.findByIdForUpdate(1001L)).thenReturn(Optional.of(task));

		TaskDispatchAckDTO ack = taskDispatchManager.markDispatched(1001L, 21L);

		assertEquals(TaskStatusEnum.DISPATCHED.name(), ack.getStatus());
		assertEquals(21L, ack.getNodeId());
		ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
		ArgumentCaptor<TaskStatusHistory> historyCaptor = ArgumentCaptor.forClass(TaskStatusHistory.class);
		verify(taskRepository).update(captor.capture());
		assertEquals(TaskStatusEnum.DISPATCHED.name(), captor.getValue().getStatus());
		assertEquals(21L, captor.getValue().getNodeId());
		verify(taskStatusHistoryRepository).save(historyCaptor.capture());
		assertEquals(TaskStatusHistoryMessageConstants.TASK_DISPATCHED, historyCaptor.getValue().getChangeReason());
	}

	@Test
	void dispatchFailedRecoverableShouldRequeueAndClearRunState() {
		Task task = buildTask(TaskStatusEnum.DISPATCHED.name());
		task.setNodeId(21L);
		task.setStartTime(LocalDateTime.now().minusMinutes(3));
		task.setEndTime(LocalDateTime.now().minusMinutes(2));
		task.setFailType("OLD_FAIL");
		task.setFailMessage("old message");
		when(taskRepository.findByIdForUpdate(1001L)).thenReturn(Optional.of(task));

		taskDispatchManager.markFailed(1001L, 21L, "DISPATCH_ERROR", "temporary dispatch failure", true);

		ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
		verify(taskRepository).update(captor.capture());
		Task updated = captor.getValue();
		assertEquals(TaskStatusEnum.QUEUED.name(), updated.getStatus());
		assertNull(updated.getNodeId());
		assertNull(updated.getStartTime());
		assertNull(updated.getEndTime());
		assertNull(updated.getFailType());
		assertNull(updated.getFailMessage());
		assertNotNull(updated.getSubmitTime());
		verifyNoInteractions(schedulerClient);
	}

	@Test
	void dispatchFailedUnrecoverableShouldFailAndReleaseReservation() {
		Task task = buildTask(TaskStatusEnum.SCHEDULED.name());
		task.setNodeId(21L);
		when(taskRepository.findByIdForUpdate(1001L)).thenReturn(Optional.of(task));

		taskDispatchManager.markFailed(1001L, 21L, "DISPATCH_ERROR", "bad dispatch payload", false);

		ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
		verify(taskRepository).update(captor.capture());
		Task updated = captor.getValue();
		assertEquals(TaskStatusEnum.FAILED.name(), updated.getStatus());
		assertEquals(21L, updated.getNodeId());
		assertEquals("DISPATCH_ERROR", updated.getFailType());
		assertEquals("bad dispatch payload", updated.getFailMessage());
		assertNotNull(updated.getEndTime());
		verifyNoInteractions(schedulerClient);
	}

	@Test
	void dispatchFailedShouldRejectWhenTaskAlreadyRunning() {
		Task task = buildTask(TaskStatusEnum.RUNNING.name());
		task.setNodeId(21L);
		when(taskRepository.findByIdForUpdate(1001L)).thenReturn(Optional.of(task));

		assertThrows(
				com.example.cae.common.exception.BizException.class,
				() -> taskDispatchManager.markFailed(1001L, 21L, "DISPATCH_ERROR", "late failure", true)
		);

		ArgumentCaptor<TaskStatusHistory> historyCaptor = ArgumentCaptor.forClass(TaskStatusHistory.class);
		verify(taskStatusHistoryRepository).save(historyCaptor.capture());
		assertEquals(
				TaskStatusHistoryMessageConstants.IGNORED_LATE_DISPATCH_FAILED_PREFIX
						+ "DISPATCH_ERROR, recoverable=true), current=RUNNING",
				historyCaptor.getValue().getChangeReason()
		);
	}

	private Task buildTask(String status) {
		Task task = new Task();
		task.setId(1001L);
		task.setTaskNo("TASK-1001");
		task.setTaskName("demo task");
		task.setUserId(7L);
		task.setSolverId(1L);
		task.setProfileId(1L);
		task.setTaskType("SIMULATION");
		task.setStatus(status);
		task.setPriority(0);
		task.setSubmitTime(LocalDateTime.now().minusMinutes(5));
		task.setDeletedFlag(0);
		return task;
	}
}
