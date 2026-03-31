package com.example.cae.task.domain.service;

import com.example.cae.common.dto.FileRuleDTO;
import com.example.cae.common.enums.TaskStatusEnum;
import com.example.cae.common.exception.BizException;
import com.example.cae.task.domain.model.Task;
import com.example.cae.task.domain.model.TaskFile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class TaskValidationDomainService {
	public void checkTaskEditable(Task task) {
		if (!Set.of(TaskStatusEnum.CREATED.name(), TaskStatusEnum.VALIDATED.name()).contains(task.getStatus())) {
			throw new BizException(400, "task status not editable");
		}
	}

	public void checkTaskCanSubmit(Task task) {
		if (!TaskStatusEnum.VALIDATED.name().equals(task.getStatus())) {
			throw new BizException(400, "task not validated");
		}
	}

	public void checkFilesMatchRules(List<TaskFile> files, List<FileRuleDTO> rules) {
		for (FileRuleDTO rule : rules) {
			if (rule.getRequiredFlag() != null && rule.getRequiredFlag() == 1) {
				boolean matched = files.stream().anyMatch(file ->
						file.matchFileKey(rule.getFileKey())
								|| file.matchOriginName(rule.getFileNamePattern())
				);
				if (!matched) {
					throw new BizException(400, "required file missing: " + rule.getFileKey());
				}
			}
		}
	}
}
