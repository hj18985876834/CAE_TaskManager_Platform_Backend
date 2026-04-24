package com.example.cae.task.infrastructure.support;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.constant.QueryValidationConstants;
import com.example.cae.common.enums.FailTypeEnum;
import com.example.cae.common.enums.TaskStatusEnum;
import com.example.cae.common.exception.BizException;
import com.example.cae.task.interfaces.request.TaskListQueryRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Locale;

@Component
public class TaskQueryBuilder {
	public TaskListQueryRequest sanitize(TaskListQueryRequest request) {
		if (request == null) {
			request = new TaskListQueryRequest();
		}
		if (request.getPageNum() == null || request.getPageNum() < 1) {
			request.setPageNum(1);
		}
		if (request.getPageSize() == null || request.getPageSize() < 1) {
			request.setPageSize(10);
		}
		request.setTaskName(normalizeOptionalText(request.getTaskName(), QueryValidationConstants.TASK_NAME_MAX_LENGTH, "taskName"));
		request.setTaskNo(normalizeBlankToNull(request.getTaskNo()));
		request.setStatus(normalizeTaskStatus(request.getStatus()));
		request.setTaskType(normalizeOptionalText(request.getTaskType(), QueryValidationConstants.TASK_TYPE_MAX_LENGTH, "taskType"));
		request.setFailType(normalizeFailType(request.getFailType()));
		validateTimeRange(request.getStartTime(), request.getEndTime());
		return request;
	}

	private String normalizeTaskStatus(String status) {
		String normalized = normalizeBlankToNull(status);
		if (normalized == null) {
			return null;
		}
		try {
			return TaskStatusEnum.valueOf(normalized.toUpperCase(Locale.ROOT)).name();
		} catch (IllegalArgumentException ex) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "invalid status: " + status);
		}
	}

	private String normalizeFailType(String failType) {
		String normalized = normalizeBlankToNull(failType);
		if (normalized == null) {
			return null;
		}
		try {
			return FailTypeEnum.valueOf(normalized.toUpperCase(Locale.ROOT)).name();
		} catch (IllegalArgumentException ex) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "invalid failType: " + failType);
		}
	}

	private String normalizeOptionalText(String value, int maxLength, String fieldName) {
		String normalized = normalizeBlankToNull(value);
		if (normalized == null) {
			return null;
		}
		if (normalized.length() > maxLength) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, fieldName + " length exceeds limit");
		}
		return normalized;
	}

	private String normalizeBlankToNull(String value) {
		if (value == null) {
			return null;
		}
		String normalized = value.trim();
		return normalized.isEmpty() ? null : normalized;
	}

	private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
		if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "startTime must be earlier than or equal to endTime");
		}
	}
}
