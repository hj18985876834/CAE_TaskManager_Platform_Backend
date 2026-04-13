package com.example.cae.nodeagent.infrastructure.support;

import com.example.cae.common.exception.BizException;
import com.example.cae.nodeagent.domain.model.ExecutionContext;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class CommandBuilder {
	public List<String> buildCommand(ExecutionContext context) {
		if (context.getCommandTemplate() == null || context.getCommandTemplate().trim().isEmpty()) {
			throw new BizException("commandTemplate is empty");
		}
		String resolvedCommand = resolveTemplate(context);

		String os = System.getProperty("os.name").toLowerCase();
		if (os.contains("win")) {
			return Arrays.asList("cmd", "/c", resolvedCommand);
		} else {
			return Arrays.asList("/bin/sh", "-c", resolvedCommand);
		}
	}

	private String resolveTemplate(ExecutionContext context) {
		String result = context.getCommandTemplate();
		Map<String, Object> variables = new LinkedHashMap<>();
		variables.put("taskId", context.getTaskId());
		variables.put("taskNo", context.getTaskNo());
		variables.put("solverId", context.getSolverId());
		variables.put("solverCode", context.getSolverCode());
		variables.put("profileId", context.getProfileId());
		variables.put("taskType", context.getTaskType());
		variables.put("workDir", context.getWorkDir());
		variables.put("taskDir", context.getTaskDir() == null || context.getTaskDir().isBlank() ? context.getWorkDir() : context.getTaskDir());
		variables.put("inputDir", context.getInputDir());
		variables.put("outputDir", context.getOutputDir());
		variables.put("logDir", context.getLogDir());
		if (context.getParams() != null && !context.getParams().isEmpty()) {
			variables.putAll(context.getParams());
		}
		for (Map.Entry<String, Object> entry : variables.entrySet()) {
			if (entry.getValue() == null) {
				continue;
			}
			result = result.replace("${" + entry.getKey() + "}", String.valueOf(entry.getValue()));
		}
		return result;
	}
}
