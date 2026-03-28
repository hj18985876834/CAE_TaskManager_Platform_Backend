package com.example.cae.solver.infrastructure.support;

import com.example.cae.solver.application.assembler.FileRuleAssembler;
import com.example.cae.solver.domain.model.SolverProfileFileRule;
import com.example.cae.solver.domain.model.SolverTaskProfile;
import com.example.cae.solver.interfaces.response.FileRuleResponse;
import com.example.cae.solver.interfaces.response.UploadSpecResponse;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Component
public class UploadSpecBuilder {
	public UploadSpecResponse build(SolverTaskProfile profile, List<SolverProfileFileRule> rules) {
		UploadSpecResponse response = new UploadSpecResponse();
		response.setProfileId(profile.getId());
		response.setProfileCode(profile.getProfileCode());
		response.setTaskType(profile.getTaskType());
		response.setProfileName(profile.getProfileName());
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
		return response;
	}
}

