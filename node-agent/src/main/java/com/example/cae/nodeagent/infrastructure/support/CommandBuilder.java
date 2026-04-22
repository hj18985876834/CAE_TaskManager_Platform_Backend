package com.example.cae.nodeagent.infrastructure.support;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.exception.BizException;
import com.example.cae.nodeagent.domain.model.ExecutionContext;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CommandBuilder {
	private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([A-Za-z][A-Za-z0-9_]*)}");
	private static final Set<String> RESERVED_VARIABLES = Set.of(
			"taskId", "taskNo", "solverId", "solverCode", "solverExecMode", "solverExecPath",
			"profileId", "taskType", "taskDir", "inputDir", "outputDir", "logDir"
	);
	private static final List<String> FORBIDDEN_COMMAND_SNIPPETS = List.of("\r", "\n", "&&", "||", ";", "|", ">", "<", "$(", "`");
	private static final List<String> FORBIDDEN_COMMAND_PREFIXES = List.of(
			"sh ",
			"bash ",
			"/bin/sh ",
			"cmd /c",
			"cmd.exe /c",
			"powershell ",
			"pwsh "
	);

	public List<String> buildCommand(ExecutionContext context) {
		if (context.getCommandTemplate() == null || context.getCommandTemplate().trim().isEmpty()) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "commandTemplate is empty");
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
		String result = context.getCommandTemplate().trim();
		validateTemplateContract(result);
		Map<String, Object> variables = new LinkedHashMap<>();
		variables.put("taskId", context.getTaskId());
		variables.put("taskNo", context.getTaskNo());
		variables.put("solverId", context.getSolverId());
		variables.put("solverCode", context.getSolverCode());
		variables.put("solverExecMode", context.getSolverExecMode());
		variables.put("solverExecPath", context.getSolverExecPath());
		variables.put("profileId", context.getProfileId());
		variables.put("taskType", context.getTaskType());
		variables.put("taskDir", context.getTaskDir() == null || context.getTaskDir().isBlank() ? context.getWorkDir() : context.getTaskDir());
		variables.put("inputDir", context.getInputDir());
		variables.put("outputDir", context.getOutputDir());
		variables.put("logDir", context.getLogDir());
		if (context.getParams() != null && !context.getParams().isEmpty()) {
			for (Map.Entry<String, Object> entry : context.getParams().entrySet()) {
				if (entry.getKey() == null || RESERVED_VARIABLES.contains(entry.getKey())) {
					continue;
				}
				variables.put(entry.getKey(), entry.getValue());
			}
		}
		for (Map.Entry<String, Object> entry : variables.entrySet()) {
			if (entry.getValue() == null) {
				continue;
			}
			result = result.replace("${" + entry.getKey() + "}", String.valueOf(entry.getValue()));
		}
		Set<String> unresolved = collectUnresolvedPlaceholders(result);
		if (!unresolved.isEmpty()) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST,
					"commandTemplate contains unresolved variables: " + String.join(", ", unresolved));
		}
		return result;
	}

	private void validateTemplateContract(String commandTemplate) {
		String lower = commandTemplate.toLowerCase(Locale.ROOT);
		for (String prefix : FORBIDDEN_COMMAND_PREFIXES) {
			if (lower.startsWith(prefix)) {
				throw new BizException(ErrorCodeConstants.BAD_REQUEST,
						"commandTemplate must be a lightweight command template, shell wrapper is not allowed");
			}
		}
		for (String snippet : FORBIDDEN_COMMAND_SNIPPETS) {
			if (commandTemplate.contains(snippet)) {
				throw new BizException(ErrorCodeConstants.BAD_REQUEST,
						"commandTemplate must stay lightweight, complex shell control is not allowed");
			}
		}
		for (String placeholder : collectUnresolvedPlaceholders(commandTemplate)) {
			if ("workDir".equalsIgnoreCase(placeholder)) {
				throw new BizException(ErrorCodeConstants.BAD_REQUEST,
						"commandTemplate does not support ${workDir}, use ${taskDir} instead");
			}
		}
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
