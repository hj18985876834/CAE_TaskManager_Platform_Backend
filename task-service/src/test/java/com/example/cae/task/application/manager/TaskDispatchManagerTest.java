package com.example.cae.task.application.manager;

import com.example.cae.common.enums.TaskStatusEnum;
import com.example.cae.task.domain.model.Task;
import com.example.cae.task.domain.repository.TaskFileRepository;
import com.example.cae.task.domain.repository.TaskRepository;
import com.example.cae.task.domain.repository.TaskStatusHistoryRepository;
import com.example.cae.task.domain.rule.TaskStatusRule;
import com.example.cae.task.domain.service.TaskStatusDomainService;
import com.example.cae.task.infrastructure.client.SchedulerClient;
import com.example.cae.task.infrastructure.client.SolverClient;
import com.example.cae.task.infrastructure.support.TaskStoragePathSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
	private SchedulerClient schedulerClient;
	@Mock
	private SolverClient solverClient;
	@Mock
	private TaskStoragePathSupport taskStoragePathSupport;

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
				taskStatusDomainService,
				schedulerClient,
				solverClient,
				taskStoragePathSupport
		);
	}

	@Test
	void markScheduledShouldBindNodeOnlyWhenTaskStillQueued() {
		Task task = buildTask(TaskStatusEnum.QUEUED.name());
		when(taskRepository.findByIdForUpdate(1001L)).thenReturn(Optional.of(task));

		boolean marked = taskDispatchManager.markScheduled(1001L, 21L);

		assertTrue(marked);
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

		boolean marked = taskDispatchManager.markScheduled(1001L, 21L);

		assertFalse(marked);
		verify(taskRepository, never()).update(org.mockito.ArgumentMatchers.any());
		verifyNoInteractions(taskStatusHistoryRepository);
	}

	@Test
	void markDispatchedShouldOnlyAdvanceScheduledTask() {
		Task task = buildTask(TaskStatusEnum.SCHEDULED.name());
		task.setNodeId(21L);
		when(taskRepository.findByIdForUpdate(1001L)).thenReturn(Optional.of(task));

		taskDispatchManager.markDispatched(1001L, 21L);

		ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
		verify(taskRepository).update(captor.capture());
		assertEquals(TaskStatusEnum.DISPATCHED.name(), captor.getValue().getStatus());
		assertEquals(21L, captor.getValue().getNodeId());
		verify(taskStatusHistoryRepository).save(org.mockito.ArgumentMatchers.any());
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

		taskDispatchManager.markFailed(1001L, "DISPATCH_ERROR", "temporary dispatch failure", true);

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
		verify(schedulerClient).releaseNodeReservation(21L);
	}

	@Test
	void dispatchFailedUnrecoverableShouldFailAndReleaseReservation() {
		Task task = buildTask(TaskStatusEnum.SCHEDULED.name());
		task.setNodeId(21L);
		when(taskRepository.findByIdForUpdate(1001L)).thenReturn(Optional.of(task));

		taskDispatchManager.markFailed(1001L, "DISPATCH_ERROR", "bad dispatch payload", false);

		ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
		verify(taskRepository).update(captor.capture());
		Task updated = captor.getValue();
		assertEquals(TaskStatusEnum.FAILED.name(), updated.getStatus());
		assertEquals(21L, updated.getNodeId());
		assertEquals("DISPATCH_ERROR", updated.getFailType());
		assertEquals("bad dispatch payload", updated.getFailMessage());
		assertNotNull(updated.getEndTime());
		verify(schedulerClient).releaseNodeReservation(21L);
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
