package com.example.cae.task.application.support;

import com.example.cae.task.interfaces.response.TaskValidateResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

@Component
public class TaskParamSchemaValidator {
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
	private static final String ERROR_CODE = "PARAM_SCHEMA_INVALID";
	private static final String RULE_KEY = "params";

	public List<TaskValidateResponse.ValidationIssue> validatePartial(String schemaText, Map<String, Object> params) {
		return validate(schemaText, params, false);
	}

	public List<TaskValidateResponse.ValidationIssue> validateComplete(String schemaText, Map<String, Object> params) {
		return validate(schemaText, params, true);
	}

	private List<TaskValidateResponse.ValidationIssue> validate(String schemaText, Map<String, Object> params, boolean enforceRequired) {
		if (schemaText == null || schemaText.isBlank()) {
			return List.of();
		}
		List<TaskValidateResponse.ValidationIssue> issues = new ArrayList<>();
		Map<String, Object> schema = parseSchema(schemaText, issues);
		if (schema == null) {
			return issues;
		}
		Map<String, Object> normalizedParams = params == null ? new LinkedHashMap<>() : new LinkedHashMap<>(params);
		validateValue(schema, normalizedParams, "$", issues, enforceRequired);
		return issues;
	}

	private Map<String, Object> parseSchema(String schemaText, List<TaskValidateResponse.ValidationIssue> issues) {
		try {
			Object parsed = OBJECT_MAPPER.readValue(schemaText, Object.class);
			if (parsed instanceof Map<?, ?> rawMap) {
				return stringKeyMap(rawMap);
			}
			issues.add(issue("$", "Parameter schema must be a JSON object"));
			return null;
		} catch (Exception ex) {
			issues.add(issue("$", "Parameter schema is invalid JSON"));
			return null;
		}
	}

	private void validateValue(Map<String, Object> schema,
							   Object value,
							   String path,
							   List<TaskValidateResponse.ValidationIssue> issues,
							   boolean enforceRequired) {
		List<String> expectedTypes = readStringList(schema.get("type"));
		if (!expectedTypes.isEmpty()) {
			String matchedType = resolveMatchedType(expectedTypes, value);
			if (matchedType == null) {
				issues.add(issue(path, "Parameter type does not match schema"));
				return;
			}
			if ("null".equals(matchedType)) {
				return;
			}
			validateConstraintsByType(matchedType, schema, value, path, issues, enforceRequired);
		} else {
			validateGenericConstraints(schema, value, path, issues, enforceRequired);
		}
	}

	private void validateConstraintsByType(String matchedType,
										   Map<String, Object> schema,
										   Object value,
										   String path,
										   List<TaskValidateResponse.ValidationIssue> issues,
										   boolean enforceRequired) {
		switch (matchedType.toLowerCase(Locale.ROOT)) {
			case "object" -> validateObject(schema, value, path, issues, enforceRequired);
			case "array" -> validateArray(schema, value, path, issues, enforceRequired);
			case "string" -> validateString(schema, value, path, issues);
			case "integer" -> validateInteger(schema, value, path, issues);
			case "number" -> validateNumber(schema, value, path, issues);
			case "boolean" -> validateBoolean(schema, value, path, issues);
			default -> validateGenericConstraints(schema, value, path, issues, enforceRequired);
		}
		validateEnumConstraint(schema, value, path, issues);
	}

	private void validateGenericConstraints(Map<String, Object> schema,
											Object value,
											String path,
											List<TaskValidateResponse.ValidationIssue> issues,
											boolean enforceRequired) {
		if (value instanceof Map<?, ?> || schema.containsKey("properties") || schema.containsKey("required")) {
			validateObject(schema, value, path, issues, enforceRequired);
			return;
		}
		if (value instanceof List<?> || schema.containsKey("items")) {
			validateArray(schema, value, path, issues, enforceRequired);
			return;
		}
		validateEnumConstraint(schema, value, path, issues);
	}

	private void validateObject(Map<String, Object> schema,
								Object value,
								String path,
								List<TaskValidateResponse.ValidationIssue> issues,
								boolean enforceRequired) {
		if (!(value instanceof Map<?, ?> rawMap)) {
			issues.add(issue(path, "Parameter must be an object"));
			return;
		}
		Map<String, Object> objectValue = stringKeyMap(rawMap);
		Map<String, Map<String, Object>> properties = readProperties(schema.get("properties"));
		if (enforceRequired) {
			for (String requiredField : readStringList(schema.get("required"))) {
				if (!objectValue.containsKey(requiredField)) {
					issues.add(issue(childPath(path, requiredField), "Missing required parameter"));
				}
			}
		}

		Object additionalProperties = schema.get("additionalProperties");
		boolean allowAdditional = !(additionalProperties instanceof Boolean bool) || bool;
		Map<String, Object> additionalSchema = additionalProperties instanceof Map<?, ?> map ? stringKeyMap(map) : null;

		for (Map.Entry<String, Object> entry : objectValue.entrySet()) {
			String key = entry.getKey();
			Object childValue = entry.getValue();
			Map<String, Object> propertySchema = properties.get(key);
			if (propertySchema != null) {
				validateValue(propertySchema, childValue, childPath(path, key), issues, enforceRequired);
				continue;
			}
			if (!allowAdditional) {
				issues.add(issue(childPath(path, key), "Parameter is not allowed by schema"));
				continue;
			}
			if (additionalSchema != null) {
				validateValue(additionalSchema, childValue, childPath(path, key), issues, enforceRequired);
			}
		}
	}

	private void validateArray(Map<String, Object> schema,
							   Object value,
							   String path,
							   List<TaskValidateResponse.ValidationIssue> issues,
							   boolean enforceRequired) {
		if (!(value instanceof List<?> values)) {
			issues.add(issue(path, "Parameter must be an array"));
			return;
		}
		Integer minItems = readInteger(schema.get("minItems"));
		Integer maxItems = readInteger(schema.get("maxItems"));
		if (minItems != null && values.size() < minItems) {
			issues.add(issue(path, "Array size is smaller than schema minimum"));
		}
		if (maxItems != null && values.size() > maxItems) {
			issues.add(issue(path, "Array size exceeds schema maximum"));
		}
		if (schema.get("items") instanceof Map<?, ?> itemsSchemaRaw) {
			Map<String, Object> itemSchema = stringKeyMap(itemsSchemaRaw);
			for (int i = 0; i < values.size(); i++) {
				validateValue(itemSchema, values.get(i), path + "[" + i + "]", issues, enforceRequired);
			}
		}
		validateEnumConstraint(schema, value, path, issues);
	}

	private void validateString(Map<String, Object> schema,
								Object value,
								String path,
								List<TaskValidateResponse.ValidationIssue> issues) {
		if (!(value instanceof String text)) {
			issues.add(issue(path, "Parameter must be a string"));
			return;
		}
		Integer minLength = readInteger(schema.get("minLength"));
		Integer maxLength = readInteger(schema.get("maxLength"));
		if (minLength != null && text.length() < minLength) {
			issues.add(issue(path, "String length is smaller than schema minimum"));
		}
		if (maxLength != null && text.length() > maxLength) {
			issues.add(issue(path, "String length exceeds schema maximum"));
		}
		String patternText = readString(schema.get("pattern"));
		if (patternText != null && !Pattern.compile(patternText).matcher(text).matches()) {
			issues.add(issue(path, "String does not match schema pattern"));
		}
	}

	private void validateInteger(Map<String, Object> schema,
								 Object value,
								 String path,
								 List<TaskValidateResponse.ValidationIssue> issues) {
		if (!isIntegerValue(value)) {
			issues.add(issue(path, "Parameter must be an integer"));
			return;
		}
		validateNumericRange(schema, toBigDecimal(value), path, issues);
	}

	private void validateNumber(Map<String, Object> schema,
								Object value,
								String path,
								List<TaskValidateResponse.ValidationIssue> issues) {
		if (!(value instanceof Number)) {
			issues.add(issue(path, "Parameter must be a number"));
			return;
		}
		validateNumericRange(schema, toBigDecimal(value), path, issues);
	}

	private void validateBoolean(Map<String, Object> schema,
								 Object value,
								 String path,
								 List<TaskValidateResponse.ValidationIssue> issues) {
		if (!(value instanceof Boolean)) {
			issues.add(issue(path, "Parameter must be a boolean"));
			return;
		}
		validateEnumConstraint(schema, value, path, issues);
	}

	private void validateNumericRange(Map<String, Object> schema,
									  BigDecimal value,
									  String path,
									  List<TaskValidateResponse.ValidationIssue> issues) {
		BigDecimal minimum = readDecimal(schema.get("minimum"));
		BigDecimal maximum = readDecimal(schema.get("maximum"));
		if (minimum != null && value.compareTo(minimum) < 0) {
			issues.add(issue(path, "Parameter is smaller than schema minimum"));
		}
		if (maximum != null && value.compareTo(maximum) > 0) {
			issues.add(issue(path, "Parameter exceeds schema maximum"));
		}
	}

	private void validateEnumConstraint(Map<String, Object> schema,
										Object value,
										String path,
										List<TaskValidateResponse.ValidationIssue> issues) {
		Object rawEnum = schema.get("enum");
		if (!(rawEnum instanceof List<?> enumValues) || enumValues.isEmpty()) {
			return;
		}
		boolean matched = enumValues.stream().anyMatch(candidate -> valueEquals(candidate, value));
		if (!matched) {
			issues.add(issue(path, "Parameter value is not in schema enum"));
		}
	}

	private String resolveMatchedType(List<String> expectedTypes, Object value) {
		for (String expectedType : expectedTypes) {
			String normalized = expectedType == null ? "" : expectedType.trim().toLowerCase(Locale.ROOT);
			if (normalized.isBlank()) {
				continue;
			}
			if (matchesType(normalized, value)) {
				return normalized;
			}
		}
		return null;
	}

	private boolean matchesType(String expectedType, Object value) {
		return switch (expectedType) {
			case "null" -> value == null;
			case "object" -> value instanceof Map<?, ?>;
			case "array" -> value instanceof List<?>;
			case "string" -> value instanceof String;
			case "integer" -> isIntegerValue(value);
			case "number" -> value instanceof Number;
			case "boolean" -> value instanceof Boolean;
			default -> true;
		};
	}

	private boolean isIntegerValue(Object value) {
		if (!(value instanceof Number number)) {
			return false;
		}
		if (number instanceof Byte || number instanceof Short || number instanceof Integer || number instanceof Long || number instanceof java.math.BigInteger) {
			return true;
		}
		if (number instanceof Float || number instanceof Double || number instanceof BigDecimal) {
			BigDecimal decimal = toBigDecimal(number);
			return decimal.stripTrailingZeros().scale() <= 0;
		}
		return false;
	}

	private BigDecimal toBigDecimal(Object value) {
		return new BigDecimal(String.valueOf(value));
	}

	private boolean valueEquals(Object left, Object right) {
		if (Objects.equals(left, right)) {
			return true;
		}
		if (left instanceof Number && right instanceof Number) {
			return toBigDecimal(left).compareTo(toBigDecimal(right)) == 0;
		}
		return Objects.equals(String.valueOf(left), String.valueOf(right));
	}

	private Map<String, Map<String, Object>> readProperties(Object rawProperties) {
		if (!(rawProperties instanceof Map<?, ?> rawMap) || rawMap.isEmpty()) {
			return Map.of();
		}
		Map<String, Map<String, Object>> properties = new LinkedHashMap<>();
		for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
			if (entry.getKey() == null || !(entry.getValue() instanceof Map<?, ?> propertySchemaRaw)) {
				continue;
			}
			properties.put(String.valueOf(entry.getKey()), stringKeyMap(propertySchemaRaw));
		}
		return properties;
	}

	private Map<String, Object> stringKeyMap(Map<?, ?> rawMap) {
		Map<String, Object> map = new LinkedHashMap<>();
		for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
			if (entry.getKey() != null) {
				map.put(String.valueOf(entry.getKey()), entry.getValue());
			}
		}
		return map;
	}

	private List<String> readStringList(Object rawValue) {
		if (rawValue == null) {
			return List.of();
		}
		if (rawValue instanceof List<?> rows) {
			return rows.stream()
					.filter(Objects::nonNull)
					.map(String::valueOf)
					.map(String::trim)
					.filter(value -> !value.isBlank())
					.toList();
		}
		String value = String.valueOf(rawValue).trim();
		return value.isBlank() ? List.of() : List.of(value);
	}

	private Integer readInteger(Object rawValue) {
		if (rawValue == null) {
			return null;
		}
		if (rawValue instanceof Number number) {
			return number.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(rawValue));
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	private BigDecimal readDecimal(Object rawValue) {
		if (rawValue == null) {
			return null;
		}
		try {
			return new BigDecimal(String.valueOf(rawValue));
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	private String readString(Object rawValue) {
		if (rawValue == null) {
			return null;
		}
		String value = String.valueOf(rawValue).trim();
		return value.isBlank() ? null : value;
	}

	private String childPath(String parent, String fieldName) {
		return "$".equals(parent) ? "$." + fieldName : parent + "." + fieldName;
	}

	private TaskValidateResponse.ValidationIssue issue(String path, String message) {
		TaskValidateResponse.ValidationIssue issue = new TaskValidateResponse.ValidationIssue();
		issue.setRuleKey(RULE_KEY);
		issue.setPath(path);
		issue.setErrorCode(ERROR_CODE);
		issue.setMessage(message);
		return issue;
	}
}
