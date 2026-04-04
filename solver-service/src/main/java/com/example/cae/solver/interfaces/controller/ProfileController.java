package com.example.cae.solver.interfaces.controller;

import com.example.cae.common.response.PageResult;
import com.example.cae.common.response.Result;
import com.example.cae.solver.application.facade.ProfileFacade;
import com.example.cae.solver.interfaces.request.CreateProfileRequest;
import com.example.cae.solver.interfaces.request.ProfilePageQueryRequest;
import com.example.cae.solver.interfaces.request.UpdateProfileRequest;
import com.example.cae.solver.interfaces.request.UpdateProfileStatusRequest;
import com.example.cae.solver.interfaces.response.FileRuleResponse;
import com.example.cae.solver.interfaces.response.ProfileCreateResponse;
import com.example.cae.solver.interfaces.response.ProfileDetailResponse;
import com.example.cae.solver.interfaces.response.ProfileListItemResponse;
import com.example.cae.solver.interfaces.response.UploadSpecResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/profiles")
public class ProfileController {
	private final ProfileFacade profileFacade;

	public ProfileController(ProfileFacade profileFacade) {
		this.profileFacade = profileFacade;
	}

	@GetMapping
	public Result<PageResult<ProfileListItemResponse>> pageProfiles(@Valid ProfilePageQueryRequest request) {
		return Result.success(profileFacade.pageProfiles(request));
	}

	@GetMapping("/{profileId}")
	public Result<ProfileDetailResponse> getProfileDetail(@PathVariable("profileId") @Positive(message = "profileId必须大于0") Long profileId) {
		return Result.success(profileFacade.getProfileDetail(profileId));
	}

	@PostMapping
	public Result<ProfileCreateResponse> createProfile(@Valid @RequestBody CreateProfileRequest request) {
		return Result.success(profileFacade.createProfile(request));
	}

	@PutMapping("/{profileId}")
	public Result<Void> updateProfile(@PathVariable("profileId") @Positive(message = "profileId必须大于0") Long profileId, @Valid @RequestBody UpdateProfileRequest request) {
		profileFacade.updateProfile(profileId, request);
		return Result.success();
	}

	@PutMapping("/{profileId}/status")
	public Result<Void> updateProfileStatus(@PathVariable("profileId") @Positive(message = "profileId必须大于0") Long profileId, @Valid @RequestBody UpdateProfileStatusRequest request) {
		profileFacade.updateProfileStatus(profileId, request);
		return Result.success();
	}

	@PostMapping("/{profileId}/status")
	public Result<Void> updateProfileStatusPost(@PathVariable("profileId") @Positive(message = "profileId必须大于0") Long profileId, @Valid @RequestBody UpdateProfileStatusRequest request) {
		profileFacade.updateProfileStatus(profileId, request);
		return Result.success();
	}

	@GetMapping("/{profileId}/upload-spec")
	public Result<UploadSpecResponse> getUploadSpec(@PathVariable("profileId") @Positive(message = "profileId必须大于0") Long profileId) {
		return Result.success(profileFacade.buildUploadSpec(profileId));
	}

	@GetMapping("/{profileId}/file-rules")
	public Result<List<FileRuleResponse>> getFileRules(@PathVariable("profileId") @Positive(message = "profileId必须大于0") Long profileId) {
		return Result.success(profileFacade.getFileRules(profileId));
	}
}

