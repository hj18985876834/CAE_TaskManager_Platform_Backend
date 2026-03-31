package com.example.cae.solver.domain.service;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.exception.BizException;
import com.example.cae.solver.domain.model.SolverProfileFileRule;
import com.example.cae.solver.domain.model.SolverTaskProfile;
import com.example.cae.solver.domain.repository.FileRuleRepository;
import com.example.cae.solver.domain.repository.ProfileRepository;
import com.example.cae.solver.interfaces.response.UploadSpecResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProfileRuleDomainService {
	private final ProfileRepository profileRepository;
	private final FileRuleRepository fileRuleRepository;

	public ProfileRuleDomainService(ProfileRepository profileRepository, FileRuleRepository fileRuleRepository) {
		this.profileRepository = profileRepository;
		this.fileRuleRepository = fileRuleRepository;
	}

	public UploadSpecResponse buildUploadSpec(SolverTaskProfile profile, List<SolverProfileFileRule> rules) {
		UploadSpecResponse response = new UploadSpecResponse();
		response.setProfileId(profile.getId());
		response.setProfileCode(profile.getProfileCode());
		response.setTaskType(profile.getTaskType());
		response.setProfileName(profile.getProfileName());
		response.setTimeoutSeconds(profile.getTimeoutSeconds());
		return response;
	}

	public void checkProfileCodeUnique(Long solverId, String profileCode) {
		if (profileRepository.findBySolverIdAndProfileCode(solverId, profileCode).isPresent()) {
			throw new BizException(ErrorCodeConstants.PROFILE_CODE_ALREADY_EXISTS, "profileCode already exists");
		}
	}

	public void checkProfileEnabled(SolverTaskProfile profile) {
		if (profile == null || !profile.isEnabled()) {
			throw new BizException(ErrorCodeConstants.PROFILE_DISABLED, "profile is not enabled");
		}
	}

	public void checkRuleConflict(Long profileId, SolverProfileFileRule rule) {
		boolean duplicated = fileRuleRepository.listByProfileId(profileId)
				.stream()
				.anyMatch(item -> item.getFileKey() != null && item.getFileKey().equals(rule.getFileKey()));
		if (duplicated) {
			throw new BizException(ErrorCodeConstants.FILE_RULE_CONFLICT, "fileKey conflict");
		}
	}
}
