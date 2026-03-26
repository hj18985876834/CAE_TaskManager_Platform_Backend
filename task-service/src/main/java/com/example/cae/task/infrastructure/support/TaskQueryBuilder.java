package com.example.cae.task.infrastructure.support;

import com.example.cae.task.interfaces.request.TaskListQueryRequest;
import org.springframework.stereotype.Component;

@Component
public class TaskQueryBuilder {
	public TaskListQueryRequest sanitize(TaskListQueryRequest request) {
		if (request == null) {
			return new TaskListQueryRequest();
		}
		if (request.getPageNum() == null || request.getPageNum() < 1) {
			request.setPageNum(1);
		}
		if (request.getPageSize() == null || request.getPageSize() < 1) {
			request.setPageSize(10);
		}
		return request;
	}
}
