package com.example.cae.task.domain.service;

import com.example.cae.common.constant.ErrorCodeConstants;
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
			throw new BizException(ErrorCodeConstants.TASK_STATUS_NOT_EDITABLE, "task status not editable");
		}
	}

	public void checkTaskCanSubmit(Task task) {
		if (!TaskStatusEnum.VALIDATED.name().equals(task.getStatus())) {
			throw new BizException(ErrorCodeConstants.TASK_NOT_VALIDATED, "task not validated");
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
					String expectedFile = rule.getFileNamePattern() != null && !rule.getFileNamePattern().isBlank()
							? rule.getFileNamePattern()
							: rule.getFileKey();
					throw new BizException(ErrorCodeConstants.TASK_VALIDATION_FAILED, "缺少必需文件 " + expectedFile);
				}
			}
		}
	}
}
