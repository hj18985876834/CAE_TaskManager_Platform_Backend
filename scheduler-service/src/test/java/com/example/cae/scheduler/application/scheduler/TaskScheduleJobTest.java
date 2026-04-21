package com.example.cae.scheduler.application.scheduler;

import com.example.cae.common.dto.TaskDTO;
import com.example.cae.scheduler.application.manager.TaskScheduleManager;
import com.example.cae.scheduler.infrastructure.client.NodeAgentClient;
import com.example.cae.scheduler.infrastructure.client.TaskClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskScheduleJobTest {
	@Mock
	private TaskClient taskClient;
	@Mock
	private NodeAgentClient nodeAgentClient;
	@Mock
	private TaskScheduleManager taskScheduleManager;

	private TaskScheduleJob taskScheduleJob;

	@BeforeEach
	void setUp() {
		taskScheduleJob = new TaskScheduleJob(taskClient, nodeAgentClient, taskScheduleManager);
	}

	@Test
	void runShouldReleaseReservationWhenMarkScheduledLostRace() {
		TaskDTO task = new TaskDTO();
		task.setTaskId(1001L);
		when(taskClient.listPendingTasks(20)).thenReturn(List.of(task));
		when(taskScheduleManager.schedule(task)).thenReturn(21L);
		when(taskClient.markTaskScheduled(1001L, 21L)).thenReturn(false);

		taskScheduleJob.run();

		verify(taskScheduleManager).releaseNodeReservation(21L);
		verify(nodeAgentClient, never()).notifyDispatch(21L, task);
		verify(taskScheduleManager, never()).confirmScheduleSuccess(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
	}
}
