package com.example.cae.task.infrastructure.support;

import com.example.cae.common.exception.ForbiddenException;
import com.example.cae.task.domain.model.Task;
import org.springframework.stereotype.Component;

@Component
public class TaskPermissionChecker {
	public void checkCanAccess(Task task, Long userId, String roleCode) {
		if ("ADMIN".equals(roleCode)) {
			return;
		}
		if (!task.isOwner(userId)) {
			throw new ForbiddenException("no permission to access task");
		}
	}
}

