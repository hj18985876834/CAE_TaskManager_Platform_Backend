package com.example.cae.solver.interfaces.internal;

import com.example.cae.common.response.Result;
import com.example.cae.solver.application.facade.ProfileFacade;
import com.example.cae.solver.infrastructure.support.ProfileTemplateContractValidator;
import com.example.cae.solver.interfaces.response.InternalProfileDetailResponse;
import com.example.cae.solver.interfaces.response.FileRuleResponse;
import com.example.cae.solver.interfaces.response.ProfileDetailResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/profiles")
public class InternalProfileController {
	private final ProfileFacade profileFacade;
	private final ProfileTemplateContractValidator profileTemplateContractValidator;

	public InternalProfileController(ProfileFacade profileFacade, ProfileTemplateContractValidator profileTemplateContractValidator) {
		this.profileFacade = profileFacade;
		this.profileTemplateContractValidator = profileTemplateContractValidator;
	}

	@GetMapping("/{profileId}")
	public Result<InternalProfileDetailResponse> getProfileDetail(@PathVariable("profileId") Long profileId) {
		ProfileDetailResponse detail = profileFacade.getProfileDetail(profileId);
		profileTemplateContractValidator.validateProfileContract(detail.getUploadMode(), detail.getCommandTemplate(), detail.getParamsSchemaJson());
		var fileRules = profileFacade.getFileRules(profileId);
		for (FileRuleResponse fileRule : fileRules) {
			profileTemplateContractValidator.validateRuleJson(fileRule.getRuleJson());
		}
		InternalProfileDetailResponse response = new InternalProfileDetailResponse();
		response.setProfileId(detail.getId());
		response.setSolverId(detail.getSolverId());
		response.setProfileCode(detail.getProfileCode());
		response.setTaskType(detail.getTaskType());
		response.setProfileName(detail.getProfileName());
		response.setCommandTemplate(detail.getCommandTemplate());
		response.setParamsSchema(detail.getParamsSchema());
		response.setParamsSchemaJson(detail.getParamsSchemaJson());
		response.setParserName(detail.getParserName());
		response.setTimeoutSeconds(detail.getTimeoutSeconds());
		response.setDescription(detail.getDescription());
		response.setEnabled(detail.getEnabled());
		response.setFileRules(fileRules);
		return Result.success(response);
	}
}
