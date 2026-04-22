package com.example.cae.solver.infrastructure.support;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.exception.BizException;
import com.example.cae.solver.domain.model.SolverTaskProfile;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ProfileTemplateContractValidator {
	private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([A-Za-z][A-Za-z0-9_]*)}");
	private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
	};
	private static final Set<String> ALLOWED_RULE_KEYS = Set.of("allowSuffix", "minCount", "maxCount", "maxSizeMb");
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
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	public void validateProfileContract(String uploadMode, String commandTemplate) {
		normalizeUploadMode(uploadMode);
		validateCommandTemplate(commandTemplate);
	}

	public void validateStoredProfileContract(SolverTaskProfile profile) {
		if (profile == null) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "profile is required");
		}
		validateProfileContract(profile.getUploadMode(), profile.getCommandTemplate());
	}

	public String normalizeUploadMode(String uploadMode) {
		String normalized = uploadMode == null ? null : uploadMode.trim().toUpperCase(Locale.ROOT);
		if (!"ZIP_ONLY".equals(normalized)) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "uploadMode only supports ZIP_ONLY");
		}
		return normalized;
	}

	public String normalizeCommandTemplate(String commandTemplate) {
		validateCommandTemplate(commandTemplate);
		return commandTemplate.trim();
	}

	public void validateCommandTemplate(String commandTemplate) {
		String normalized = commandTemplate == null ? null : commandTemplate.trim();
		if (normalized == null || normalized.isEmpty()) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "commandTemplate is required");
		}
		String lower = normalized.toLowerCase(Locale.ROOT);
		for (String prefix : FORBIDDEN_COMMAND_PREFIXES) {
			if (lower.startsWith(prefix)) {
				throw new BizException(ErrorCodeConstants.BAD_REQUEST,
						"commandTemplate must be a lightweight command template, shell wrapper is not allowed");
			}
		}
		for (String snippet : FORBIDDEN_COMMAND_SNIPPETS) {
			if (normalized.contains(snippet)) {
				throw new BizException(ErrorCodeConstants.BAD_REQUEST,
						"commandTemplate must stay lightweight, complex shell control is not allowed");
			}
		}
		Set<String> placeholders = collectPlaceholders(normalized);
		for (String placeholder : placeholders) {
			if ("workDir".equalsIgnoreCase(placeholder)) {
				throw new BizException(ErrorCodeConstants.BAD_REQUEST,
						"commandTemplate does not support ${workDir}, use ${taskDir} instead");
			}
		}
	}

	public void validateRuleJson(String ruleJson) {
		if (ruleJson == null || ruleJson.isBlank()) {
			return;
		}
		Map<String, Object> ruleMap;
		try {
			ruleMap = OBJECT_MAPPER.readValue(ruleJson, MAP_TYPE);
		} catch (Exception ex) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "ruleJson must be a valid JSON object");
		}
		if (ruleMap == null || ruleMap.isEmpty()) {
			return;
		}
		for (String key : ruleMap.keySet()) {
			if (!ALLOWED_RULE_KEYS.contains(key)) {
				throw new BizException(ErrorCodeConstants.BAD_REQUEST,
						"ruleJson only supports allowSuffix, minCount, maxCount, maxSizeMb");
			}
		}
		validateAllowSuffix(ruleMap.get("allowSuffix"));
		Integer minCount = parseNonNegativeInteger(ruleMap.get("minCount"), "minCount", true);
		Integer maxCount = parseNonNegativeInteger(ruleMap.get("maxCount"), "maxCount", true);
		parseNonNegativeInteger(ruleMap.get("maxSizeMb"), "maxSizeMb", false);
		if (minCount != null && maxCount != null && maxCount < minCount) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "ruleJson maxCount cannot be smaller than minCount");
		}
	}

	private void validateAllowSuffix(Object allowSuffixValue) {
		if (allowSuffixValue == null) {
			return;
		}
		if (!(allowSuffixValue instanceof List<?> rows)) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "ruleJson allowSuffix must be an array");
		}
		for (Object row : rows) {
			if (row == null || String.valueOf(row).isBlank()) {
				throw new BizException(ErrorCodeConstants.BAD_REQUEST, "ruleJson allowSuffix cannot contain blank values");
			}
		}
	}

	private Integer parseNonNegativeInteger(Object value, String fieldName, boolean allowZero) {
		if (value == null) {
			return null;
		}
		Integer parsed;
		if (value instanceof Number number) {
			parsed = number.intValue();
		} else {
			try {
				parsed = Integer.parseInt(String.valueOf(value));
			} catch (NumberFormatException ex) {
				throw new BizException(ErrorCodeConstants.BAD_REQUEST, "ruleJson " + fieldName + " must be an integer");
			}
		}
		if (parsed < 0 || (!allowZero && parsed == 0)) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST,
					"ruleJson " + fieldName + (allowZero ? " must be >= 0" : " must be > 0"));
		}
		return parsed;
	}

	private Set<String> collectPlaceholders(String commandTemplate) {
		Set<String> placeholders = new LinkedHashSet<>();
		Matcher matcher = PLACEHOLDER_PATTERN.matcher(commandTemplate);
		while (matcher.find()) {
			String placeholder = matcher.group(1);
			if (placeholder != null && !placeholder.isBlank()) {
				placeholders.add(placeholder);
			}
		}
		return placeholders;
	}
}
