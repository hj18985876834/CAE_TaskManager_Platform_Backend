package com.example.cae.solver.application.facade;

import com.example.cae.common.response.PageResult;
import com.example.cae.solver.application.service.FileRuleAppService;
import com.example.cae.solver.application.service.ProfileAppService;
import com.example.cae.solver.application.service.UploadSpecAppService;
import com.example.cae.solver.interfaces.request.CreateFileRuleRequest;
import com.example.cae.solver.interfaces.request.CreateProfileRequest;
import com.example.cae.solver.interfaces.request.ProfilePageQueryRequest;
import com.example.cae.solver.interfaces.request.UpdateFileRuleRequest;
import com.example.cae.solver.interfaces.request.UpdateProfileRequest;
import com.example.cae.solver.interfaces.request.UpdateProfileStatusRequest;
import com.example.cae.solver.interfaces.response.FileRuleResponse;
import com.example.cae.solver.interfaces.response.ProfileDetailResponse;
import com.example.cae.solver.interfaces.response.ProfileListItemResponse;
import com.example.cae.solver.interfaces.response.UploadSpecResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProfileFacade {
	private final ProfileAppService profileAppService;
	private final FileRuleAppService fileRuleAppService;
	private final UploadSpecAppService uploadSpecAppService;

	public ProfileFacade(ProfileAppService profileAppService, FileRuleAppService fileRuleAppService, UploadSpecAppService uploadSpecAppService) {
		this.profileAppService = profileAppService;
		this.fileRuleAppService = fileRuleAppService;
		this.uploadSpecAppService = uploadSpecAppService;
	}

	public PageResult<ProfileListItemResponse> pageProfiles(ProfilePageQueryRequest request) {
		return profileAppService.pageProfiles(request);
	}

	public ProfileDetailResponse getProfileDetail(Long profileId) {
		return profileAppService.getProfileDetail(profileId);
	}

	public void createProfile(CreateProfileRequest request) {
		profileAppService.createProfile(request);
	}

	public void updateProfile(Long profileId, UpdateProfileRequest request) {
		profileAppService.updateProfile(profileId, request);
	}

	public void updateProfileStatus(Long profileId, UpdateProfileStatusRequest request) {
		profileAppService.updateProfileStatus(profileId, request);
	}

	public List<FileRuleResponse> getFileRules(Long profileId) {
		return profileAppService.getFileRules(profileId);
	}

	public UploadSpecResponse buildUploadSpec(Long profileId) {
		return uploadSpecAppService.buildUploadSpec(profileId);
	}

	public void createFileRule(Long profileId, CreateFileRuleRequest request) {
		fileRuleAppService.createFileRule(profileId, request);
	}

	public void updateFileRule(Long ruleId, UpdateFileRuleRequest request) {
		fileRuleAppService.updateFileRule(ruleId, request);
	}

	public void deleteFileRule(Long ruleId) {
		fileRuleAppService.deleteFileRule(ruleId);
	}
}
