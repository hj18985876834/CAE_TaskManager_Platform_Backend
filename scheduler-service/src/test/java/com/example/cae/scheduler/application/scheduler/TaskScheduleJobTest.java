package com.example.cae.scheduler.application.scheduler;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.dto.TaskBasicDTO;
import com.example.cae.common.dto.TaskDispatchAckDTO;
import com.example.cae.common.dto.TaskDTO;
import com.example.cae.common.dto.TaskScheduleClaimDTO;
import com.example.cae.common.exception.BizException;
import com.example.cae.scheduler.application.manager.TaskScheduleManager;
import com.example.cae.scheduler.application.service.DispatchFailureReleaseException;
import com.example.cae.scheduler.infrastructure.client.NodeAgentClient;
import com.example.cae.scheduler.infrastructure.client.TaskClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
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
		when(taskClient.markTaskScheduled(1001L, 21L)).thenReturn(scheduleClaim(false, 22L, "SCHEDULED"));

		taskScheduleJob.run();

		verify(taskScheduleManager).releaseNodeReservation(21L, 1001L);
		verify(nodeAgentClient, never()).notifyDispatch(21L, task);
		verify(taskScheduleManager, never()).confirmScheduleSuccess(eq(1001L), eq(21L), org.mockito.ArgumentMatchers.anyString());
	}

	@Test
	void runShouldRecordFormalRejectedClaimMessageWhenTaskClaimedByAnotherScheduler() {
		TaskDTO task = new TaskDTO();
		task.setTaskId(1001L);
		when(taskClient.listPendingTasks(20)).thenReturn(List.of(task));
		when(taskScheduleManager.schedule(task)).thenReturn(21L);
		when(taskClient.markTaskScheduled(1001L, 21L)).thenReturn(scheduleClaim(false, 22L, "SCHEDULED"));

		taskScheduleJob.run();

		verify(taskScheduleManager).releaseNodeReservation(21L, 1001L);
		verify(taskScheduleManager).recordScheduleFailure(
				1001L,
				21L,
				ScheduleAuditMessageConstants.SCHEDULE_CLAIM_REJECTED_ALREADY_CLAIMED_BY_ANOTHER_SCHEDULER);
	}

	@Test
	void runShouldFallbackToGenericRejectedClaimMessageForUnexpectedStatus() {
		TaskDTO task = new TaskDTO();
		task.setTaskId(1001L);
		when(taskClient.listPendingTasks(20)).thenReturn(List.of(task));
		when(taskScheduleManager.schedule(task)).thenReturn(21L);
		when(taskClient.markTaskScheduled(1001L, 21L)).thenReturn(scheduleClaim(false, null, "QUEUED"));

		taskScheduleJob.run();

		verify(taskScheduleManager).releaseNodeReservation(21L, 1001L);
		verify(taskScheduleManager).recordScheduleFailure(
				1001L,
				21L,
				ScheduleAuditMessageConstants.SCHEDULE_CLAIM_REJECTED);
	}

	@Test
	void runShouldNotRecordRejectedClaimFailureForSameNodeDispatchedIdempotentPath() {
		TaskDTO task = new TaskDTO();
		task.setTaskId(1001L);
		when(taskClient.listPendingTasks(20)).thenReturn(List.of(task));
		when(taskScheduleManager.schedule(task)).thenReturn(21L);
		when(taskClient.markTaskScheduled(1001L, 21L)).thenReturn(scheduleClaim(false, 21L, "DISPATCHED"));

		taskScheduleJob.run();

		verify(taskScheduleManager, never()).releaseNodeReservation(21L, 1001L);
		verify(taskScheduleManager, never()).recordScheduleFailure(eq(1001L), eq(21L), anyString());
		verify(nodeAgentClient, never()).notifyDispatch(21L, task);
	}

	@Test
	void runShouldSkipGenericConfirmFailureRecordAfterDispatchFailedAlreadySettledTask() {
		TaskDTO task = new TaskDTO();
		task.setTaskId(1001L);
		when(taskClient.listPendingTasks(20)).thenReturn(List.of(task));
		when(taskScheduleManager.schedule(task)).thenReturn(21L);
		when(taskClient.markTaskScheduled(1001L, 21L)).thenReturn(scheduleClaim(true, 21L, "SCHEDULED"));
		when(taskClient.markTaskDispatched(1001L, 21L)).thenThrow(
				new BizException(ErrorCodeConstants.TASK_STATUS_TRANSFER_ILLEGAL, "illegal status for dispatch confirm: QUEUED"));
		when(taskClient.getTaskBasics(List.of(1001L))).thenReturn(Map.of(1001L, taskBasic(1001L, null, "QUEUED")));

		taskScheduleJob.run();

		verify(taskScheduleManager, never()).recordScheduleFailure(
				eq(1001L),
				eq(21L),
				contains("node accepted task, mark-dispatched confirm failed"));
		verify(taskScheduleManager, never()).confirmScheduleSuccess(eq(1001L), eq(21L), org.mockito.ArgumentMatchers.anyString());
	}

	@Test
	void runShouldKeepGenericConfirmFailureRecordWhenTaskStillPendingSameNodeFollowup() {
		TaskDTO task = new TaskDTO();
		task.setTaskId(1001L);
		when(taskClient.listPendingTasks(20)).thenReturn(List.of(task));
		when(taskScheduleManager.schedule(task)).thenReturn(21L);
		when(taskClient.markTaskScheduled(1001L, 21L)).thenReturn(scheduleClaim(true, 21L, "SCHEDULED"));
		when(taskClient.markTaskDispatched(1001L, 21L)).thenThrow(
				new BizException(ErrorCodeConstants.TASK_STATUS_TRANSFER_ILLEGAL, "illegal status for dispatch confirm: SCHEDULED"));
		when(taskClient.getTaskBasics(List.of(1001L))).thenReturn(Map.of(1001L, taskBasic(1001L, 21L, "SCHEDULED")));

		taskScheduleJob.run();

		verify(taskScheduleManager).recordScheduleFailure(
				eq(1001L),
				eq(21L),
				eq(ScheduleAuditMessageConstants.NODE_ACCEPTED_MARK_DISPATCHED_CONFIRM_FAILED_PREFIX
						+ "illegal status for dispatch confirm: SCHEDULED"));
		verify(taskScheduleManager, never()).confirmScheduleSuccess(eq(1001L), eq(21L), org.mockito.ArgumentMatchers.anyString());
	}

	@Test
	void runShouldRecordFormalAcceptedDispatchConfirmFailureMessageWithDefaultReason() {
		TaskDTO task = new TaskDTO();
		task.setTaskId(1001L);
		when(taskClient.listPendingTasks(20)).thenReturn(List.of(task));
		when(taskScheduleManager.schedule(task)).thenReturn(21L);
		when(taskClient.markTaskScheduled(1001L, 21L)).thenReturn(scheduleClaim(true, 21L, "SCHEDULED"));
		when(taskClient.markTaskDispatched(1001L, 21L)).thenThrow(
				new BizException(ErrorCodeConstants.TASK_STATUS_TRANSFER_ILLEGAL, ""));
		when(taskClient.getTaskBasics(List.of(1001L))).thenReturn(Map.of(1001L, taskBasic(1001L, 21L, "DISPATCHED")));

		taskScheduleJob.run();

		verify(taskScheduleManager).recordScheduleFailure(
				1001L,
				21L,
				ScheduleAuditMessageConstants.NODE_ACCEPTED_MARK_DISPATCHED_CONFIRM_FAILED_PREFIX
						+ ScheduleAuditMessageConstants.MARK_DISPATCHED_CONFIRM_FAILED_DEFAULT_REASON);
	}

	@Test
	void runShouldRecordFormalSuccessMessageWhenNodeAlreadyRunning() {
		TaskDTO task = new TaskDTO();
		task.setTaskId(1001L);
		TaskDispatchAckDTO ack = new TaskDispatchAckDTO();
		ack.setTaskId(1001L);
		ack.setNodeId(21L);
		ack.setStatus("RUNNING");
		when(taskClient.listPendingTasks(20)).thenReturn(List.of(task));
		when(taskScheduleManager.schedule(task)).thenReturn(21L);
		when(taskClient.markTaskScheduled(1001L, 21L)).thenReturn(scheduleClaim(true, 21L, "SCHEDULED"));
		when(taskClient.markTaskDispatched(1001L, 21L)).thenReturn(ack);

		taskScheduleJob.run();

		verify(taskScheduleManager).confirmScheduleSuccess(
				1001L,
				21L,
				ScheduleAuditMessageConstants.TASK_DISPATCHED_ALREADY_RUNNING);
	}

	@Test
	void runShouldRecordFormalRecoveredSuccessMessageWhenAckRecoveredAsFailed() {
		TaskDTO task = new TaskDTO();
		task.setTaskId(1001L);
		when(taskClient.listPendingTasks(20)).thenReturn(List.of(task));
		when(taskScheduleManager.schedule(task)).thenReturn(21L);
		when(taskClient.markTaskScheduled(1001L, 21L)).thenReturn(scheduleClaim(true, 21L, "SCHEDULED"));
		when(taskClient.markTaskDispatched(1001L, 21L)).thenThrow(
				new BizException(ErrorCodeConstants.BAD_GATEWAY, "mark-dispatched gateway timeout"));
		when(taskClient.getTaskBasics(List.of(1001L))).thenReturn(Map.of(1001L, taskBasic(1001L, 21L, "FAILED")));

		taskScheduleJob.run();

		verify(taskScheduleManager).confirmScheduleSuccess(
				1001L,
				21L,
				ScheduleAuditMessageConstants.MARK_DISPATCHED_ACK_RECOVERED_ALREADY_FAILED);
	}

	@Test
	void runShouldNotRecordGenericFailureAgainWhenDispatchFailureReleaseAlreadyAudited() {
		TaskDTO task = new TaskDTO();
		task.setTaskId(1001L);
		when(taskClient.listPendingTasks(20)).thenReturn(List.of(task));
		when(taskScheduleManager.schedule(task)).thenReturn(21L);
		when(taskClient.markTaskScheduled(1001L, 21L)).thenReturn(scheduleClaim(true, 21L, "SCHEDULED"));
		doThrow(new BizException(ErrorCodeConstants.BAD_GATEWAY, "node-agent dispatch request failed"))
				.when(nodeAgentClient).notifyDispatch(21L, task);
		when(taskScheduleManager.handleDispatchFailure(eq(1001L), eq(21L), eq("DISPATCH_ERROR"), anyString(), anyBoolean()))
				.thenThrow(new DispatchFailureReleaseException(
						"reservation release failed after dispatch-failed: scheduler release failed",
						new BizException(ErrorCodeConstants.BAD_GATEWAY, "scheduler release failed")
				));

		taskScheduleJob.run();

		verify(taskScheduleManager, never()).recordScheduleFailure(
				eq(1001L),
				eq(21L),
				contains("reservation release failed after dispatch-failed"));
	}

	private TaskScheduleClaimDTO scheduleClaim(boolean claimed, Long nodeId, String status) {
		TaskScheduleClaimDTO claim = new TaskScheduleClaimDTO();
		claim.setClaimed(claimed);
		claim.setTaskId(1001L);
		claim.setNodeId(nodeId);
		claim.setStatus(status);
		return claim;
	}

	private TaskBasicDTO taskBasic(Long taskId, Long nodeId, String status) {
		TaskBasicDTO taskBasic = new TaskBasicDTO();
		taskBasic.setTaskId(taskId);
		taskBasic.setNodeId(nodeId);
		taskBasic.setStatus(status);
		return taskBasic;
	}
}
