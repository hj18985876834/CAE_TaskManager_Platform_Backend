package com.example.cae.nodeagent.infrastructure.support;

import com.example.cae.common.exception.BizException;
import com.example.cae.nodeagent.domain.model.ExecutionContext;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CommandBuilder {
	private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([A-Za-z][A-Za-z0-9_]*)}");

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
		variables.put("solverExecMode", context.getSolverExecMode());
		variables.put("solverExecPath", context.getSolverExecPath());
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
		Set<String> unresolved = collectUnresolvedPlaceholders(result);
		if (!unresolved.isEmpty()) {
			throw new BizException("commandTemplate contains unresolved variables: " + String.join(", ", unresolved));
		}
		return result;
	}

	private Set<String> collectUnresolvedPlaceholders(String command) {
		Set<String> placeholders = new LinkedHashSet<>();
		Matcher matcher = PLACEHOLDER_PATTERN.matcher(command);
		while (matcher.find()) {
			String placeholder = matcher.group(1);
			if (placeholder != null && !placeholder.isBlank()) {
				placeholders.add(placeholder);
			}
		}
		return placeholders;
	}
}
