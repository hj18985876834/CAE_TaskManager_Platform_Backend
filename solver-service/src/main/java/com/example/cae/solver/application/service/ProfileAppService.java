package com.example.cae.solver.application.service;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.exception.BizException;
import com.example.cae.common.response.PageResult;
import com.example.cae.solver.application.assembler.FileRuleAssembler;
import com.example.cae.solver.application.assembler.ProfileAssembler;
import com.example.cae.solver.domain.model.SolverDefinition;
import com.example.cae.solver.domain.model.SolverTaskProfile;
import com.example.cae.solver.domain.repository.FileRuleRepository;
import com.example.cae.solver.domain.repository.ProfileRepository;
import com.example.cae.solver.domain.repository.SolverRepository;
import com.example.cae.solver.domain.service.ProfileRuleDomainService;
import com.example.cae.solver.infrastructure.support.ProfileTemplateContractValidator;
import com.example.cae.solver.interfaces.request.CreateProfileRequest;
import com.example.cae.solver.interfaces.request.ProfilePageQueryRequest;
import com.example.cae.solver.interfaces.request.UpdateProfileRequest;
import com.example.cae.solver.interfaces.request.UpdateProfileStatusRequest;
import com.example.cae.solver.interfaces.response.FileRuleResponse;
import com.example.cae.solver.interfaces.response.ProfileCreateResponse;
import com.example.cae.solver.interfaces.response.ProfileDetailResponse;
import com.example.cae.solver.interfaces.response.ProfileListItemResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProfileAppService {
	private final SolverRepository solverRepository;
	private final ProfileRepository profileRepository;
	private final FileRuleRepository fileRuleRepository;
	private final ProfileRuleDomainService profileRuleDomainService;
	private final ProfileTemplateContractValidator profileTemplateContractValidator;

	public ProfileAppService(SolverRepository solverRepository,
							 ProfileRepository profileRepository,
							 FileRuleRepository fileRuleRepository,
							 ProfileRuleDomainService profileRuleDomainService,
							 ProfileTemplateContractValidator profileTemplateContractValidator) {
		this.solverRepository = solverRepository;
		this.profileRepository = profileRepository;
		this.fileRuleRepository = fileRuleRepository;
		this.profileRuleDomainService = profileRuleDomainService;
		this.profileTemplateContractValidator = profileTemplateContractValidator;
	}

	public PageResult<ProfileListItemResponse> pageProfiles(ProfilePageQueryRequest request) {
		int pageNum = request == null || request.getPageNum() == null || request.getPageNum() < 1 ? 1 : request.getPageNum();
		int pageSize = request == null || request.getPageSize() == null || request.getPageSize() < 1 ? 10 : request.getPageSize();
		long offset = (long) (pageNum - 1) * pageSize;

		PageResult<SolverTaskProfile> page = profileRepository.page(request, offset, pageSize);
		List<ProfileListItemResponse> records = page.getRecords().stream().map(ProfileAssembler::toListItemResponse).toList();
		return PageResult.of(page.getTotal(), pageNum, pageSize, records);
	}

	public ProfileDetailResponse getProfileDetail(Long profileId) {
		SolverTaskProfile profile = profileRepository.findById(profileId).orElseThrow(() -> new BizException(ErrorCodeConstants.PROFILE_NOT_FOUND, "profile not found"));
		ProfileDetailResponse response = ProfileAssembler.toDetailResponse(profile);
		response.setFileRules(getFileRules(profileId));
		return response;
	}

	public ProfileCreateResponse createProfile(CreateProfileRequest request) {
		SolverDefinition solver = solverRepository.findById(request.getSolverId()).orElseThrow(() -> new BizException(ErrorCodeConstants.SOLVER_NOT_FOUND, "solver not found"));
		if (!solver.isEnabled()) {
			throw new BizException(ErrorCodeConstants.SOLVER_DISABLED, "solver is disabled");
		}
		profileTemplateContractValidator.validateProfileContract(request.getUploadMode(), request.getCommandTemplate());
		profileRuleDomainService.checkProfileCodeUnique(request.getSolverId(), request.getProfileCode());
		SolverTaskProfile profile = ProfileAssembler.toProfile(request);
		profile.setUploadMode(profileTemplateContractValidator.normalizeUploadMode(request.getUploadMode()));
		profile.setCommandTemplate(profileTemplateContractValidator.normalizeCommandTemplate(request.getCommandTemplate()));
		if (request.getEnabled() != null && request.getEnabled() == 0) {
			profile.disable();
		} else {
			profile.enable();
		}
		profileRepository.save(profile);
		return ProfileAssembler.toCreateResponse(profile);
	}

	public void updateProfile(Long profileId, UpdateProfileRequest request) {
		SolverTaskProfile profile = profileRepository.findById(profileId).orElseThrow(() -> new BizException(ErrorCodeConstants.PROFILE_NOT_FOUND, "profile not found"));
		profileTemplateContractValidator.validateProfileContract(request.getUploadMode(), request.getCommandTemplate());
		profile.setTaskType(request.getTaskType());
		profile.setProfileName(request.getProfileName());
		profile.setUploadMode(profileTemplateContractValidator.normalizeUploadMode(request.getUploadMode()));
		profile.setCommandTemplate(profileTemplateContractValidator.normalizeCommandTemplate(request.getCommandTemplate()));
		if (request.getParamsSchema() != null && !request.getParamsSchema().isBlank()) {
			profile.setParamsSchemaJson(request.getParamsSchema());
		} else {
			profile.setParamsSchemaJson(request.getParamsSchemaJson());
		}
		profile.setParserName(request.getParserName());
		profile.changeTimeout(request.getTimeoutSeconds());
		profile.setDescription(request.getDescription());
		profileRepository.update(profile);
	}

	public void updateProfileStatus(Long profileId, UpdateProfileStatusRequest request) {
		SolverTaskProfile profile = profileRepository.findById(profileId).orElseThrow(() -> new BizException(ErrorCodeConstants.PROFILE_NOT_FOUND, "profile not found"));
		if (request != null && request.getEnabled() != null && request.getEnabled() == 1) {
			profile.enable();
		} else {
			profile.disable();
		}
		profileRepository.update(profile);
	}

	public List<FileRuleResponse> getFileRules(Long profileId) {
		return fileRuleRepository.listByProfileId(profileId).stream().map(FileRuleAssembler::toResponse).toList();
	}
}
