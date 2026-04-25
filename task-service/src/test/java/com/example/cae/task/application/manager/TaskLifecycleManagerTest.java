package com.example.cae.task.application.manager;

import com.example.cae.common.enums.TaskStatusEnum;
import com.example.cae.task.application.assembler.TaskAssembler;
import com.example.cae.task.domain.model.Task;
import com.example.cae.task.domain.repository.TaskFileRepository;
import com.example.cae.task.domain.repository.TaskLogRepository;
import com.example.cae.task.domain.repository.TaskRepository;
import com.example.cae.task.domain.repository.TaskResultFileRepository;
import com.example.cae.task.domain.repository.TaskResultSummaryRepository;
import com.example.cae.task.domain.repository.TaskStatusHistoryRepository;
import com.example.cae.task.domain.rule.TaskStatusRule;
import com.example.cae.task.domain.service.TaskDomainService;
import com.example.cae.task.domain.service.TaskStatusDomainService;
import com.example.cae.task.domain.service.TaskValidationDomainService;
import com.example.cae.task.infrastructure.client.SchedulerClient;
import com.example.cae.task.infrastructure.client.SolverClient;
import com.example.cae.task.infrastructure.storage.TaskFileStorageService;
import com.example.cae.task.application.support.TaskParamSchemaValidator;
import com.example.cae.task.infrastructure.support.TaskNoGenerator;
import com.example.cae.task.infrastructure.support.TaskStoragePathSupport;
import com.example.cae.task.interfaces.request.StatusReportRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskLifecycleManagerTest {
	@Mock
	private TaskRepository taskRepository;
	@Mock
	private TaskFileRepository taskFileRepository;
	@Mock
	private TaskStatusHistoryRepository taskStatusHistoryRepository;
	@Mock
	private TaskLogRepository taskLogRepository;
	@Mock
	private TaskResultSummaryRepository taskResultSummaryRepository;
	@Mock
	private TaskResultFileRepository taskResultFileRepository;
	@Mock
	private TaskDomainService taskDomainService;
	@Mock
	private TaskValidationDomainService taskValidationDomainService;
	@Mock
	private TaskValidationManager taskValidationManager;
	@Mock
	private TaskFileStorageService taskFileStorageService;
	@Mock
	private TaskParamSchemaValidator taskParamSchemaValidator;
	@Mock
	private TaskAssembler taskAssembler;
	@Mock
	private TaskNoGenerator taskNoGenerator;
	@Mock
	private SchedulerClient schedulerClient;
	@Mock
	private SolverClient solverClient;
	@Mock
	private TaskStoragePathSupport taskStoragePathSupport;

	private TaskLifecycleManager taskLifecycleManager;

	@BeforeEach
	void setUp() {
		TaskStatusDomainService taskStatusDomainService = new TaskStatusDomainService(
				new TaskStatusRule(),
				taskStatusHistoryRepository
		);
		taskLifecycleManager = new TaskLifecycleManager(
				taskRepository,
				taskFileRepository,
				taskStatusHistoryRepository,
				taskLogRepository,
				taskResultSummaryRepository,
				taskResultFileRepository,
				taskDomainService,
				taskStatusDomainService,
				taskValidationDomainService,
				taskValidationManager,
				taskFileStorageService,
				taskParamSchemaValidator,
				taskAssembler,
				taskNoGenerator,
				schedulerClient,
				solverClient,
				taskStoragePathSupport
		);
	}

	@Test
	void reportRunningShouldReleaseNodeReservation() {
		Task task = new Task();
		task.setId(1001L);
		task.setStatus(TaskStatusEnum.DISPATCHED.name());
		task.setNodeId(21L);
		when(taskRepository.findByIdForUpdate(1001L)).thenReturn(Optional.of(task));

		StatusReportRequest request = new StatusReportRequest();
		request.setNodeId(21L);
		request.setToStatus(TaskStatusEnum.RUNNING.name());
		request.setChangeReason("node-agent start execute");

		taskLifecycleManager.reportStatus(1001L, request);

		ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
		verify(taskRepository).update(captor.capture());
		assertEquals(TaskStatusEnum.RUNNING.name(), captor.getValue().getStatus());
		assertNotNull(captor.getValue().getStartTime());
		verify(schedulerClient).releaseNodeReservation(21L, 1001L);
		verify(taskStatusHistoryRepository).save(org.mockito.ArgumentMatchers.any());
	}
}
