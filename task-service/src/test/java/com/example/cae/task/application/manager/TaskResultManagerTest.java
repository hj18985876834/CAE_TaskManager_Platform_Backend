package com.example.cae.task.application.manager;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.enums.TaskStatusEnum;
import com.example.cae.common.exception.BizException;
import com.example.cae.task.domain.model.Task;
import com.example.cae.task.domain.model.TaskStatusHistory;
import com.example.cae.task.domain.repository.TaskLogRepository;
import com.example.cae.task.domain.repository.TaskRepository;
import com.example.cae.task.domain.repository.TaskResultFileRepository;
import com.example.cae.task.domain.repository.TaskResultSummaryRepository;
import com.example.cae.task.domain.repository.TaskStatusHistoryRepository;
import com.example.cae.task.domain.service.TaskStatusDomainService;
import com.example.cae.task.application.support.TaskStatusHistoryMessageConstants;
import com.example.cae.task.infrastructure.client.SchedulerClient;
import com.example.cae.task.infrastructure.support.TaskPathResolver;
import com.example.cae.task.infrastructure.support.TaskStoragePathSupport;
import com.example.cae.task.interfaces.request.ResultSummaryReportRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
	private TaskStatusHistoryRepository taskStatusHistoryRepository;
	@Mock
	private TaskStatusDomainService taskStatusDomainService;
	@Mock
	private SchedulerClient schedulerClient;
	@Mock
	private TaskStoragePathSupport taskStoragePathSupport;
	@Mock
	private TaskPathResolver taskPathResolver;

	private TaskResultManager taskResultManager;

	@BeforeEach
	void setUp() {
		taskResultManager = new TaskResultManager(
				taskRepository,
				taskLogRepository,
				taskResultSummaryRepository,
				taskResultFileRepository,
				taskStatusHistoryRepository,
				taskStatusDomainService,
				schedulerClient,
				taskStoragePathSupport
				,
				taskPathResolver
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

	@Test
	void finishTaskShouldRecordStructuredAuditWhenLateTerminalCallbackIgnored() {
		Task task = new Task();
		task.setId(1L);
		task.setStatus(TaskStatusEnum.SUCCESS.name());
		when(taskRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(task));

		taskResultManager.finishTask(1L, TaskStatusEnum.SUCCESS.name());

		ArgumentCaptor<TaskStatusHistory> historyCaptor = ArgumentCaptor.forClass(TaskStatusHistory.class);
		verify(taskStatusHistoryRepository).save(historyCaptor.capture());
		assertEquals(
				TaskStatusHistoryMessageConstants.IGNORED_LATE_PREFIX
						+ TaskStatusHistoryMessageConstants.MARK_FINISHED_ACTION
						+ TaskStatusHistoryMessageConstants.REQUESTED_EQUALS
						+ TaskStatusEnum.SUCCESS.name()
						+ TaskStatusHistoryMessageConstants.CURRENT_EQUALS
						+ TaskStatusEnum.SUCCESS.name(),
				historyCaptor.getValue().getChangeReason()
		);
		verifyNoInteractions(taskStatusDomainService);
	}

	@Test
	void saveResultSummaryShouldRejectLateCallbackWithStructuredAudit() {
		Task task = new Task();
		task.setId(1L);
		task.setStatus(TaskStatusEnum.FAILED.name());
		when(taskRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(task));

		ResultSummaryReportRequest request = new ResultSummaryReportRequest();
		request.setSuccessFlag(0);

		BizException exception = assertThrows(BizException.class, () -> taskResultManager.saveResultSummary(1L, request));

		assertEquals(ErrorCodeConstants.TASK_STATUS_TRANSFER_ILLEGAL, exception.getCode());
		ArgumentCaptor<TaskStatusHistory> historyCaptor = ArgumentCaptor.forClass(TaskStatusHistory.class);
		verify(taskStatusHistoryRepository).save(historyCaptor.capture());
		assertEquals(
				TaskStatusHistoryMessageConstants.IGNORED_LATE_PREFIX
						+ TaskStatusHistoryMessageConstants.RESULT_SUMMARY_REPORT_ACTION
						+ TaskStatusHistoryMessageConstants.CURRENT_EQUALS
						+ TaskStatusEnum.FAILED.name(),
				historyCaptor.getValue().getChangeReason()
		);
		verifyNoInteractions(taskResultSummaryRepository);
	}
}
