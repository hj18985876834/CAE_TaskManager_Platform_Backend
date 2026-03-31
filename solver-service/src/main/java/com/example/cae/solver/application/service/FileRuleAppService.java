package com.example.cae.solver.application.service;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.exception.BizException;
import com.example.cae.solver.application.assembler.FileRuleAssembler;
import com.example.cae.solver.domain.model.SolverProfileFileRule;
import com.example.cae.solver.domain.repository.FileRuleRepository;
import com.example.cae.solver.domain.repository.ProfileRepository;
import com.example.cae.solver.domain.service.ProfileRuleDomainService;
import com.example.cae.solver.infrastructure.support.ProfileRuleValidator;
import com.example.cae.solver.interfaces.request.CreateFileRuleRequest;
import com.example.cae.solver.interfaces.request.UpdateFileRuleRequest;
import com.example.cae.solver.interfaces.response.FileRuleCreateResponse;
import org.springframework.stereotype.Service;

@Service
public class FileRuleAppService {
	private final ProfileRepository profileRepository;
	private final FileRuleRepository fileRuleRepository;
	private final ProfileRuleDomainService profileRuleDomainService;
	private final ProfileRuleValidator profileRuleValidator;

	public FileRuleAppService(ProfileRepository profileRepository, FileRuleRepository fileRuleRepository, ProfileRuleDomainService profileRuleDomainService, ProfileRuleValidator profileRuleValidator) {
		this.profileRepository = profileRepository;
		this.fileRuleRepository = fileRuleRepository;
		this.profileRuleDomainService = profileRuleDomainService;
		this.profileRuleValidator = profileRuleValidator;
	}

	public FileRuleCreateResponse createFileRule(Long profileId, CreateFileRuleRequest request) {
		profileRepository.findById(profileId).orElseThrow(() -> new BizException(ErrorCodeConstants.PROFILE_NOT_FOUND, "profile not found"));
		profileRuleValidator.validateCreateRule(request);
		SolverProfileFileRule rule = FileRuleAssembler.toRule(profileId, request);
		profileRuleDomainService.checkRuleConflict(profileId, rule);
		fileRuleRepository.save(rule);
		return FileRuleAssembler.toCreateResponse(rule);
	}

	public void updateFileRule(Long ruleId, UpdateFileRuleRequest request) {
		profileRuleValidator.validateUpdateRule(request);
		SolverProfileFileRule rule = fileRuleRepository.findById(ruleId).orElseThrow(() -> new BizException(ErrorCodeConstants.FILE_RULE_NOT_FOUND, "rule not found"));
		rule.setFileNamePattern(request.getFileNamePattern());
		rule.setFileType(request.getFileType());
		rule.setRequiredFlag(request.getRequiredFlag());
		rule.setSortOrder(request.getSortOrder());
		if (request.getDescription() != null && !request.getDescription().isBlank()) {
			rule.setRemark(request.getDescription());
		} else {
			rule.setRemark(request.getRemark());
		}
		fileRuleRepository.update(rule);
	}

	public void deleteFileRule(Long ruleId) {
		fileRuleRepository.delete(ruleId);
	}
}
