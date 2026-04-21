package com.example.cae.task.application.manager;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.enums.TaskStatusEnum;
import com.example.cae.common.exception.BizException;
import com.example.cae.task.domain.model.Task;
import com.example.cae.task.domain.repository.TaskLogRepository;
import com.example.cae.task.domain.repository.TaskRepository;
import com.example.cae.task.domain.repository.TaskResultFileRepository;
import com.example.cae.task.domain.repository.TaskResultSummaryRepository;
import com.example.cae.task.domain.service.TaskStatusDomainService;
import com.example.cae.task.infrastructure.support.TaskStoragePathSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskResultManagerTest {
	@Mock
	private TaskRepository taskRepository;
	@Mock
	private TaskLogRepository taskLogRepository;
	@Mock
	private TaskResultSummaryRepository taskResultSummaryRepository;
	@Mock
	private TaskResultFileRepository taskResultFileRepository;
	@Mock
	private TaskStatusDomainService taskStatusDomainService;
	@Mock
	private TaskStoragePathSupport taskStoragePathSupport;

	private TaskResultManager taskResultManager;

	@BeforeEach
	void setUp() {
		taskResultManager = new TaskResultManager(
				taskRepository,
				taskLogRepository,
				taskResultSummaryRepository,
				taskResultFileRepository,
				taskStatusDomainService,
				taskStoragePathSupport
		);
	}

	@Test
	void finishTaskShouldRejectUnsupportedFinalStatus() {
		Task task = new Task();
		task.setId(1L);
		task.setStatus(TaskStatusEnum.RUNNING.name());
		when(taskRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(task));

		BizException exception = assertThrows(BizException.class, () -> taskResultManager.finishTask(1L, "FAILED"));

		assertEquals(ErrorCodeConstants.BAD_REQUEST, exception.getCode());
		verify(taskRepository).findByIdForUpdate(1L);
		verifyNoInteractions(taskStatusDomainService);
	}
}
