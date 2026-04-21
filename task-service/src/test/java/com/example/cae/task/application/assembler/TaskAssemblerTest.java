package com.example.cae.task.application.assembler;

import com.example.cae.common.enums.TaskStatusEnum;
import com.example.cae.task.domain.model.Task;
import com.example.cae.task.interfaces.response.TaskDetailResponse;
import com.example.cae.task.interfaces.response.TaskListItemResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskAssemblerTest {
	private final TaskAssembler taskAssembler = new TaskAssembler();

	@Test
	void toDetailResponseShouldExposeCanCancelForQueuedTask() {
		TaskDetailResponse response = taskAssembler.toDetailResponse(buildTask(TaskStatusEnum.QUEUED.name()));

		assertTrue(response.getCanCancel());
		assertFalse(response.getCanRetry());
	}

	@Test
	void toListItemResponseShouldExposeCanRetryForFailedTask() {
		TaskListItemResponse response = taskAssembler.toListItemResponse(buildTask(TaskStatusEnum.FAILED.name()));

		assertFalse(response.getCanCancel());
		assertTrue(response.getCanRetry());
	}

	private Task buildTask(String status) {
		Task task = new Task();
		task.setId(1L);
		task.setTaskNo("TASK-001");
		task.setTaskName("demo");
		task.setSolverId(10L);
		task.setProfileId(20L);
		task.setTaskType("CAE");
		task.setPriority(5);
		task.setStatus(status);
		return task;
	}
}
