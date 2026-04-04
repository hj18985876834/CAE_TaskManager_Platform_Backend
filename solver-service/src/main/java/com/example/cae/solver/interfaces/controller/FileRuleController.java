package com.example.cae.solver.interfaces.controller;

import com.example.cae.common.response.Result;
import com.example.cae.solver.application.facade.ProfileFacade;
import com.example.cae.solver.interfaces.request.CreateFileRuleRequest;
import com.example.cae.solver.interfaces.request.UpdateFileRuleRequest;
import com.example.cae.solver.interfaces.response.FileRuleCreateResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api")
public class FileRuleController {
	private final ProfileFacade profileFacade;

	public FileRuleController(ProfileFacade profileFacade) {
		this.profileFacade = profileFacade;
	}

	@PostMapping("/profiles/{profileId}/file-rules")
	public Result<FileRuleCreateResponse> createFileRule(@PathVariable("profileId") @Positive(message = "profileId必须大于0") Long profileId, @Valid @RequestBody CreateFileRuleRequest request) {
		return Result.success(profileFacade.createFileRule(profileId, request));
	}

	@PutMapping("/file-rules/{id}")
	public Result<Void> updateFileRule(@PathVariable("id") @Positive(message = "id必须大于0") Long id, @Valid @RequestBody UpdateFileRuleRequest request) {
		profileFacade.updateFileRule(id, request);
		return Result.success();
	}

	@DeleteMapping("/file-rules/{id}")
	public Result<Void> deleteFileRule(@PathVariable("id") @Positive(message = "id必须大于0") Long id) {
		profileFacade.deleteFileRule(id);
		return Result.success();
	}
}

