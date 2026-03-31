package com.example.cae.solver.application.service;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.exception.BizException;
import com.example.cae.solver.domain.model.SolverProfileFileRule;
import com.example.cae.solver.domain.model.SolverTaskProfile;
import com.example.cae.solver.domain.repository.FileRuleRepository;
import com.example.cae.solver.domain.repository.ProfileRepository;
import com.example.cae.solver.domain.service.ProfileRuleDomainService;
import com.example.cae.solver.infrastructure.support.UploadSpecBuilder;
import com.example.cae.solver.interfaces.response.UploadSpecResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UploadSpecAppService {
	private final ProfileRepository profileRepository;
	private final FileRuleRepository fileRuleRepository;
	private final ProfileRuleDomainService profileRuleDomainService;
	private final UploadSpecBuilder uploadSpecBuilder;

	public UploadSpecAppService(ProfileRepository profileRepository, FileRuleRepository fileRuleRepository, ProfileRuleDomainService profileRuleDomainService, UploadSpecBuilder uploadSpecBuilder) {
		this.profileRepository = profileRepository;
		this.fileRuleRepository = fileRuleRepository;
		this.profileRuleDomainService = profileRuleDomainService;
		this.uploadSpecBuilder = uploadSpecBuilder;
	}

	public UploadSpecResponse buildUploadSpec(Long profileId) {
		SolverTaskProfile profile = profileRepository.findById(profileId).orElseThrow(() -> new BizException(ErrorCodeConstants.PROFILE_NOT_FOUND, "profile not found"));
		profileRuleDomainService.checkProfileEnabled(profile);
		List<SolverProfileFileRule> rules = fileRuleRepository.listByProfileId(profileId);
		return uploadSpecBuilder.build(profile, rules);
	}
}
