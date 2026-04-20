package com.example.cae.solver.infrastructure.support;

import com.example.cae.solver.application.assembler.FileRuleAssembler;
import com.example.cae.solver.domain.model.SolverProfileFileRule;
import com.example.cae.solver.domain.model.SolverTaskProfile;
import com.example.cae.solver.interfaces.response.FileRuleResponse;
import com.example.cae.solver.interfaces.response.UploadSpecResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Component
public class UploadSpecBuilder {
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

	public UploadSpecResponse build(SolverTaskProfile profile, List<SolverProfileFileRule> rules) {
		UploadSpecResponse response = new UploadSpecResponse();
		response.setProfileId(profile.getId());
		response.setProfileCode(profile.getProfileCode());
		response.setTaskType(profile.getTaskType());
		response.setProfileName(profile.getProfileName());
		response.setUploadMode(profile.getUploadMode());
		response.setParamsSchema(profile.getParamsSchemaJson());
		response.setParamsSchemaJson(profile.getParamsSchemaJson());
		response.setTimeoutSeconds(profile.getTimeoutSeconds());
		response.setDescription(profile.getDescription());

		List<FileRuleResponse> requiredFiles = rules.stream()
				.filter(SolverProfileFileRule::isRequired)
				.sorted(Comparator.comparing(SolverProfileFileRule::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
				.map(FileRuleAssembler::toResponse)
				.toList();

		List<FileRuleResponse> optionalFiles = rules.stream()
				.filter(rule -> !rule.isRequired())
				.sorted(Comparator.comparing(SolverProfileFileRule::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
				.map(FileRuleAssembler::toResponse)
				.toList();

		response.setRequiredFiles(requiredFiles);
		response.setOptionalFiles(optionalFiles);
		response.setFileRules(Stream.concat(requiredFiles.stream(), optionalFiles.stream()).toList());
		response.setArchiveRule(buildArchiveRule(rules));
		return response;
	}

	private UploadSpecResponse.ArchiveRule buildArchiveRule(List<SolverProfileFileRule> rules) {
		SolverProfileFileRule archiveSource = rules.stream()
				.filter(this::isArchiveRule)
				.sorted(Comparator.comparing(SolverProfileFileRule::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
				.findFirst()
				.orElse(null);
		if (archiveSource == null) {
			return null;
		}
		Map<String, Object> ruleJson = parseRuleJson(archiveSource.getRuleJson());
		UploadSpecResponse.ArchiveRule archiveRule = new UploadSpecResponse.ArchiveRule();
		archiveRule.setFileKey(archiveSource.getFileKey() == null || archiveSource.getFileKey().isBlank()
				? "input_archive"
				: archiveSource.getFileKey());
		Object allowSuffix = ruleJson.get("allowSuffix");
		if (allowSuffix instanceof List<?> suffixRows && !suffixRows.isEmpty()) {
			archiveRule.setAllowSuffix(suffixRows.stream()
					.filter(java.util.Objects::nonNull)
					.map(String::valueOf)
					.toList());
		} else {
			archiveRule.setAllowSuffix(java.util.List.of("zip"));
		}
		Object maxSizeMb = ruleJson.get("maxSizeMb");
		if (maxSizeMb instanceof Number number) {
			archiveRule.setMaxSizeMb(number.intValue());
		} else if (maxSizeMb != null) {
			try {
				archiveRule.setMaxSizeMb(Integer.parseInt(String.valueOf(maxSizeMb)));
			} catch (NumberFormatException ignored) {
				archiveRule.setMaxSizeMb(2048);
			}
		} else {
			archiveRule.setMaxSizeMb(2048);
		}
		return archiveRule;
	}

	private boolean isArchiveRule(SolverProfileFileRule rule) {
		if (rule == null) {
			return false;
		}
		if (rule.getFileKey() != null && "input_archive".equalsIgnoreCase(rule.getFileKey())) {
			return true;
		}
		return rule.getFileType() != null && "ZIP".equalsIgnoreCase(rule.getFileType());
	}

	private Map<String, Object> parseRuleJson(String ruleJson) {
		if (ruleJson == null || ruleJson.isBlank()) {
			return Map.of();
		}
		try {
			return OBJECT_MAPPER.readValue(ruleJson, MAP_TYPE);
		} catch (Exception ignored) {
			return Map.of();
		}
	}
}

