package com.example.cae.solver.interfaces.controller;

import com.example.cae.common.response.Result;
import com.example.cae.solver.application.facade.ProfileFacade;
import com.example.cae.solver.interfaces.request.CreateFileRuleRequest;
import com.example.cae.solver.interfaces.request.UpdateFileRuleRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class FileRuleController {
	private final ProfileFacade profileFacade;

	public FileRuleController(ProfileFacade profileFacade) {
		this.profileFacade = profileFacade;
	}

	@PostMapping("/profiles/{profileId}/file-rules")
	public Result<Void> createFileRule(@PathVariable("profileId") Long profileId, @RequestBody CreateFileRuleRequest request) {
		profileFacade.createFileRule(profileId, request);
		return Result.success();
	}

	@PutMapping("/file-rules/{id}")
	public Result<Void> updateFileRule(@PathVariable("id") Long id, @RequestBody UpdateFileRuleRequest request) {
		profileFacade.updateFileRule(id, request);
		return Result.success();
	}

	@DeleteMapping("/file-rules/{id}")
	public Result<Void> deleteFileRule(@PathVariable("id") Long id) {
		profileFacade.deleteFileRule(id);
		return Result.success();
	}
}

