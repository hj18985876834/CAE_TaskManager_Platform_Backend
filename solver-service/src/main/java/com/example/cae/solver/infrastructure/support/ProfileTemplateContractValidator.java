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
	private static final Set<String> RULE_METADATA_KEYS = Set.of(
			"allowSuffix",
			"minCount",
			"maxCount",
			"min",
			"max",
			"maxSizeMb",
			"deriveParam",
			"deriveParams"
	);
	private static final Set<String> ALLOWED_SCOPE_SELECTOR_KEYS = Set.of(
			"solverId", "solverIds",
			"profileId", "profileIds",
			"taskType", "taskTypes",
			"solverCode", "solverCodes",
			"solverExecMode", "solverExecModes",
			"solverExecPath", "solverExecPaths"
	);
	private static final Set<String> RESERVED_TEMPLATE_VARIABLES = Set.of(
			"taskId", "taskNo", "solverId", "solverCode", "solverExecMode", "solverExecPath",
			"profileId", "taskType", "taskDir", "inputDir", "outputDir", "logDir"
	);
	private static final Set<String> ALLOWED_DERIVE_SOURCES = Set.of(
			"literal",
			"fileNameRegex",
			"relativePathRegex",
			"fileContentRegex"
	);
	private static final Set<String> ALLOWED_DERIVE_KEYS = Set.of(
			"name", "source", "value", "pattern", "group", "required",
			"trim", "stripQuotes", "sanitizeRegex", "preprocess"
	);
	private static final Set<String> ALLOWED_PREPROCESS_KEYS = Set.of("pattern", "replacement");
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
		validateProfileContract(uploadMode, commandTemplate, null);
	}

	public void validateProfileContract(String uploadMode, String commandTemplate, String paramsSchemaText) {
		normalizeUploadMode(uploadMode);
		validateParamsSchema(paramsSchemaText);
		validateCommandTemplate(commandTemplate, paramsSchemaText);
	}

	public void validateStoredProfileContract(SolverTaskProfile profile) {
		if (profile == null) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "profile is required");
		}
		validateProfileContract(profile.getUploadMode(), profile.getCommandTemplate(), profile.getParamsSchemaJson());
	}

	public String normalizeUploadMode(String uploadMode) {
		String normalized = uploadMode == null ? null : uploadMode.trim().toUpperCase(Locale.ROOT);
		if (!"ZIP_ONLY".equals(normalized)) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "uploadMode only supports ZIP_ONLY");
		}
		return normalized;
	}

	public String normalizeCommandTemplate(String commandTemplate) {
		return validateCommandTemplateSyntax(commandTemplate);
	}

	private void validateParamsSchema(String paramsSchemaText) {
		if (paramsSchemaText == null || paramsSchemaText.isBlank()) {
			return;
		}
		Object parsed;
		try {
			parsed = OBJECT_MAPPER.readValue(paramsSchemaText, Object.class);
		} catch (Exception ex) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "paramsSchema must be a valid JSON object");
		}
		if (!(parsed instanceof Map<?, ?> rawMap)) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "paramsSchema must be a JSON object");
		}
		validateParamsSchemaNode(toStringKeyMap(rawMap), "$");
	}

	private void validateParamsSchemaNode(Map<String, Object> schema, String path) {
		Object properties = schema.get("properties");
		if (properties != null) {
			if (!(properties instanceof Map<?, ?> rawProperties)) {
				throw new BizException(ErrorCodeConstants.BAD_REQUEST, "paramsSchema " + path + ".properties must be an object");
			}
			for (Map.Entry<?, ?> entry : rawProperties.entrySet()) {
				if (entry.getKey() == null || String.valueOf(entry.getKey()).isBlank()) {
					throw new BizException(ErrorCodeConstants.BAD_REQUEST, "paramsSchema property name cannot be blank");
				}
				if (!(entry.getValue() instanceof Map<?, ?> propertySchema)) {
					throw new BizException(ErrorCodeConstants.BAD_REQUEST, "paramsSchema property schema must be an object: " + entry.getKey());
				}
				validateParamsSchemaNode(toStringKeyMap(propertySchema), path + ".properties." + entry.getKey());
			}
		}

		Object required = schema.get("required");
		if (required != null && !(required instanceof List<?>)) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "paramsSchema " + path + ".required must be an array");
		}
		if (required instanceof List<?> rows) {
			for (Object row : rows) {
				if (row == null || String.valueOf(row).isBlank()) {
					throw new BizException(ErrorCodeConstants.BAD_REQUEST, "paramsSchema required item cannot be blank");
				}
			}
		}

		Object pattern = schema.get("pattern");
		if (pattern != null) {
			validateParamsSchemaRegex(pattern, path + ".pattern");
		}
		Object items = schema.get("items");
		if (items != null) {
			if (!(items instanceof Map<?, ?> itemSchema)) {
				throw new BizException(ErrorCodeConstants.BAD_REQUEST, "paramsSchema " + path + ".items must be an object");
			}
			validateParamsSchemaNode(toStringKeyMap(itemSchema), path + ".items");
		}
		Object additionalProperties = schema.get("additionalProperties");
		if (additionalProperties instanceof Map<?, ?> additionalSchema) {
			validateParamsSchemaNode(toStringKeyMap(additionalSchema), path + ".additionalProperties");
		} else if (additionalProperties != null && !(additionalProperties instanceof Boolean)) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "paramsSchema " + path + ".additionalProperties must be boolean or object");
		}
	}

	private void validateParamsSchemaRegex(Object value, String fieldName) {
		String pattern = trimToNull(value);
		if (pattern == null) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "paramsSchema " + fieldName + " cannot be blank");
		}
		try {
			Pattern.compile(pattern);
		} catch (Exception ex) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "paramsSchema " + fieldName + " is invalid");
		}
	}

	public void validateCommandTemplate(String commandTemplate) {
		validateCommandTemplate(commandTemplate, null);
	}

	public void validateCommandTemplate(String commandTemplate, String paramsSchemaText) {
		String normalized = validateCommandTemplateSyntax(commandTemplate);
		validateCommandPlaceholders(normalized, paramsSchemaText);
	}

	private String validateCommandTemplateSyntax(String commandTemplate) {
		String normalized = commandTemplate == null ? null : commandTemplate.trim();
		if (normalized == null || normalized.isEmpty()) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "commandTemplate is required");
		}
		String lower = normalized.toLowerCase(Locale.ROOT);
		for (String prefix : FORBIDDEN_COMMAND_PREFIXES) {
			if (lower.startsWith(prefix)) {
				throw new BizException(
						ErrorCodeConstants.BAD_REQUEST,
						"commandTemplate must be a lightweight command template, shell wrapper is not allowed"
				);
			}
		}
		for (String snippet : FORBIDDEN_COMMAND_SNIPPETS) {
			if (normalized.contains(snippet)) {
				throw new BizException(
						ErrorCodeConstants.BAD_REQUEST,
						"commandTemplate must stay lightweight, complex shell control is not allowed"
				);
			}
		}
		Set<String> placeholders = collectPlaceholders(normalized);
		for (String placeholder : placeholders) {
			if ("workDir".equalsIgnoreCase(placeholder)) {
				throw new BizException(
						ErrorCodeConstants.BAD_REQUEST,
						"commandTemplate does not support ${workDir}, use ${taskDir} instead"
				);
			}
		}
		return normalized;
	}

	private void validateCommandPlaceholders(String commandTemplate, String paramsSchemaText) {
		Set<String> placeholders = collectPlaceholders(commandTemplate);
		if (placeholders.isEmpty()) {
			return;
		}
		Set<String> customPlaceholders = new LinkedHashSet<>();
		for (String placeholder : placeholders) {
			if (!RESERVED_TEMPLATE_VARIABLES.contains(placeholder)) {
				customPlaceholders.add(placeholder);
			}
		}
		if (customPlaceholders.isEmpty()) {
			return;
		}
		Set<String> paramNames = readParamsSchemaProperties(paramsSchemaText, true);
		customPlaceholders.removeAll(paramNames);
		if (!customPlaceholders.isEmpty()) {
			throw new BizException(
					ErrorCodeConstants.BAD_REQUEST,
					"commandTemplate contains unknown variables: " + String.join(", ", customPlaceholders)
			);
		}
	}

	private Set<String> readParamsSchemaProperties(String paramsSchemaText, boolean requiredForTemplateParams) {
		if (paramsSchemaText == null || paramsSchemaText.isBlank()) {
			return Set.of();
		}
		Map<String, Object> schemaMap;
		try {
			schemaMap = OBJECT_MAPPER.readValue(paramsSchemaText, MAP_TYPE);
		} catch (Exception ex) {
			if (requiredForTemplateParams) {
				throw new BizException(ErrorCodeConstants.BAD_REQUEST, "paramsSchema must be a valid JSON object when commandTemplate uses task params");
			}
			return Set.of();
		}
		if (schemaMap == null) {
			return Set.of();
		}
		Object properties = schemaMap.get("properties");
		if (!(properties instanceof Map<?, ?> rawProperties)) {
			return Set.of();
		}
		Set<String> names = new LinkedHashSet<>();
		for (Object key : rawProperties.keySet()) {
			if (key != null && !String.valueOf(key).isBlank()) {
				names.add(String.valueOf(key));
			}
		}
		return names;
	}

	public void validateFileRuleContract(Integer requiredFlag, String ruleJson) {
		validateRuleJson(ruleJson);
		if (requiredFlag == null || requiredFlag != 1 || ruleJson == null || ruleJson.isBlank()) {
			return;
		}
		Map<String, Object> ruleMap;
		try {
			ruleMap = OBJECT_MAPPER.readValue(ruleJson, MAP_TYPE);
		} catch (Exception ex) {
			return;
		}
		Integer maxCount = parseFirstNonNegativeInteger(ruleMap, "maxCount", "max");
		if (maxCount != null && maxCount == 0) {
			throw new BizException(
					ErrorCodeConstants.BAD_REQUEST,
					"ruleJson maxCount cannot be 0 when requiredFlag is 1"
			);
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
		for (Map.Entry<String, Object> entry : ruleMap.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if (key == null || key.isBlank()) {
				throw new BizException(ErrorCodeConstants.BAD_REQUEST, "ruleJson key cannot be blank");
			}
			switch (key) {
				case "allowSuffix" -> validateAllowSuffix(value);
				case "minCount" -> parseNonNegativeInteger(value, "minCount", true);
				case "maxCount" -> parseNonNegativeInteger(value, "maxCount", true);
				case "min" -> parseNonNegativeInteger(value, "min", true);
				case "max" -> parseNonNegativeInteger(value, "max", true);
				case "maxSizeMb" -> parseNonNegativeInteger(value, "maxSizeMb", false);
				case "deriveParam" -> validateDeriveParam(value, false);
			case "deriveParams" -> validateDeriveParams(value);
				default -> validateScopeSelector(key, value);
			}
		}
		if (ruleMap.containsKey("deriveParam") && ruleMap.containsKey("deriveParams")) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "ruleJson cannot contain both deriveParam and deriveParams");
		}
		Integer minCount = parseFirstNonNegativeInteger(ruleMap, "minCount", "min");
		Integer maxCount = parseFirstNonNegativeInteger(ruleMap, "maxCount", "max");
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

	private void validateScopeSelector(String key, Object value) {
		if (!ALLOWED_SCOPE_SELECTOR_KEYS.contains(key)) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "ruleJson unsupported scope selector: " + key);
		}
		List<String> values = readStringList(value);
		if (values.isEmpty()) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "ruleJson " + key + " must be a non-empty string or array");
		}
	}

	private void validateDeriveParams(Object value) {
		if (value == null) {
			return;
		}
		if (!(value instanceof List<?> rows)) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "ruleJson deriveParams must be an array");
		}
		for (Object row : rows) {
			validateDeriveParam(row, true);
		}
	}

	private void validateDeriveParam(Object value, boolean fromList) {
		if (!(value instanceof Map<?, ?> rawMap)) {
			throw new BizException(
					ErrorCodeConstants.BAD_REQUEST,
					"ruleJson " + (fromList ? "deriveParams item" : "deriveParam") + " must be an object"
			);
		}
		Map<String, Object> deriveMap = toStringKeyMap(rawMap);
		for (String key : deriveMap.keySet()) {
			if (!ALLOWED_DERIVE_KEYS.contains(key)) {
				throw new BizException(ErrorCodeConstants.BAD_REQUEST, "ruleJson deriveParam unsupported key: " + key);
			}
		}
		String name = trimToNull(deriveMap.get("name"));
		if (name == null) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "ruleJson deriveParam.name is required");
		}
		String source = trimToNull(deriveMap.get("source"));
		if (source == null) {
			source = "fileContentRegex";
		}
		if (!ALLOWED_DERIVE_SOURCES.contains(source)) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "ruleJson deriveParam.source is unsupported");
		}
		if ("literal".equals(source)) {
			if (deriveMap.get("value") == null) {
				throw new BizException(ErrorCodeConstants.BAD_REQUEST, "ruleJson deriveParam.value is required");
			}
		} else {
			String pattern = trimToNull(deriveMap.get("pattern"));
			if (pattern == null) {
				throw new BizException(ErrorCodeConstants.BAD_REQUEST, "ruleJson deriveParam.pattern is required");
			}
			validateOptionalRegex(pattern, "deriveParam.pattern");
		}
		parseOptionalInteger(deriveMap.get("group"), "deriveParam.group", true);
		validateOptionalRegex(deriveMap.get("sanitizeRegex"), "deriveParam.sanitizeRegex");
		validatePreprocessRules(deriveMap.get("preprocess"));
	}

	private void validatePreprocessRules(Object value) {
		if (value == null) {
			return;
		}
		if (!(value instanceof List<?> rows)) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "ruleJson deriveParam.preprocess must be an array");
		}
		for (Object row : rows) {
			if (!(row instanceof Map<?, ?> rawMap)) {
				throw new BizException(ErrorCodeConstants.BAD_REQUEST, "ruleJson deriveParam.preprocess item must be an object");
			}
			Map<String, Object> preprocess = toStringKeyMap(rawMap);
			for (String key : preprocess.keySet()) {
				if (!ALLOWED_PREPROCESS_KEYS.contains(key)) {
					throw new BizException(ErrorCodeConstants.BAD_REQUEST, "ruleJson deriveParam.preprocess unsupported key: " + key);
				}
			}
			String pattern = trimToNull(preprocess.get("pattern"));
			if (pattern == null) {
				throw new BizException(ErrorCodeConstants.BAD_REQUEST, "ruleJson deriveParam.preprocess.pattern is required");
			}
			validateOptionalRegex(pattern, "deriveParam.preprocess.pattern");
			if (preprocess.containsKey("replacement") && preprocess.get("replacement") == null) {
				throw new BizException(ErrorCodeConstants.BAD_REQUEST, "ruleJson deriveParam.preprocess.replacement cannot be null");
			}
		}
	}

	private void validateOptionalRegex(Object value, String fieldName) {
		String pattern = trimToNull(value);
		if (pattern == null) {
			return;
		}
		try {
			Pattern.compile(pattern);
		} catch (Exception ex) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "ruleJson " + fieldName + " is invalid");
		}
	}

	private Integer parseOptionalInteger(Object value, String fieldName, boolean allowZero) {
		if (value == null) {
			return null;
		}
		return parseNonNegativeInteger(value, fieldName, allowZero);
	}

	private Integer parseFirstNonNegativeInteger(Map<String, Object> map, String primaryKey, String aliasKey) {
		Integer primary = parseNonNegativeInteger(map.get(primaryKey), primaryKey, true);
		if (primary != null) {
			return primary;
		}
		return parseNonNegativeInteger(map.get(aliasKey), aliasKey, true);
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
			throw new BizException(
					ErrorCodeConstants.BAD_REQUEST,
					"ruleJson " + fieldName + (allowZero ? " must be >= 0" : " must be > 0")
			);
		}
		return parsed;
	}

	private List<String> readStringList(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof List<?> rows) {
			return rows.stream()
					.filter(item -> item != null && !String.valueOf(item).isBlank())
					.map(String::valueOf)
					.toList();
		}
		String value = String.valueOf(raw).trim();
		if (value.isBlank()) {
			return List.of();
		}
		return List.of(value);
	}

	private Map<String, Object> toStringKeyMap(Map<?, ?> rawMap) {
		java.util.LinkedHashMap<String, Object> normalized = new java.util.LinkedHashMap<>();
		for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
			if (entry.getKey() != null) {
				normalized.put(String.valueOf(entry.getKey()), entry.getValue());
			}
		}
		return normalized;
	}

	private String trimToNull(Object value) {
		if (value == null) {
			return null;
		}
		String normalized = String.valueOf(value).trim();
		return normalized.isBlank() ? null : normalized;
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
