package com.example.cae.solver.application.assembler;

import com.example.cae.solver.domain.model.SolverTaskProfile;
import com.example.cae.solver.infrastructure.persistence.entity.SolverTaskProfilePO;
import com.example.cae.solver.interfaces.request.CreateProfileRequest;
import com.example.cae.solver.interfaces.response.ProfileDetailResponse;
import com.example.cae.solver.interfaces.response.ProfileListItemResponse;

public class ProfileAssembler {
	private ProfileAssembler() {
	}

	public static SolverTaskProfile toProfile(CreateProfileRequest request) {
		SolverTaskProfile profile = new SolverTaskProfile();
		profile.setSolverId(request.getSolverId());
		profile.setProfileCode(request.getProfileCode());
		profile.setTaskType(request.getTaskType());
		profile.setProfileName(request.getProfileName());
		profile.setCommandTemplate(request.getCommandTemplate());
		profile.setParamsSchemaJson(resolveParamsSchema(request.getParamsSchema(), request.getParamsSchemaJson()));
		profile.setParserName(request.getParserName());
		profile.setTimeoutSeconds(request.getTimeoutSeconds());
		profile.setDescription(request.getDescription());
		if (request.getEnabled() != null) {
			profile.setEnabled(request.getEnabled());
		}
		return profile;
	}

	public static ProfileListItemResponse toListItemResponse(SolverTaskProfile profile) {
			   ProfileListItemResponse response = new ProfileListItemResponse();
			   response.setId(profile.getId());
			   response.setProfileId(profile.getId());
			   response.setSolverId(profile.getSolverId());
			   response.setProfileCode(profile.getProfileCode());
			   response.setTaskType(profile.getTaskType());
			   response.setProfileName(profile.getProfileName());
			   response.setParamsSchema(profile.getParamsSchemaJson());
			   response.setCommandTemplate(profile.getCommandTemplate());
			   response.setParserName(profile.getParserName());
			   response.setTimeoutSeconds(profile.getTimeoutSeconds());
			   response.setDescription(profile.getDescription());
			   response.setEnabled(profile.getEnabled());
			   return response;
	}

	public static ProfileDetailResponse toDetailResponse(SolverTaskProfile profile) {
		ProfileDetailResponse response = new ProfileDetailResponse();
		response.setId(profile.getId());
		response.setProfileId(profile.getId());
		response.setSolverId(profile.getSolverId());
		response.setProfileCode(profile.getProfileCode());
		response.setTaskType(profile.getTaskType());
		response.setProfileName(profile.getProfileName());
		response.setCommandTemplate(profile.getCommandTemplate());
		response.setParamsSchema(profile.getParamsSchemaJson());
		response.setParamsSchemaJson(profile.getParamsSchemaJson());
		response.setParserName(profile.getParserName());
		response.setTimeoutSeconds(profile.getTimeoutSeconds());
		response.setDescription(profile.getDescription());
		response.setEnabled(profile.getEnabled());
		return response;
	}

	private static String resolveParamsSchema(String paramsSchema, String paramsSchemaJson) {
		if (paramsSchema != null && !paramsSchema.isBlank()) {
			return paramsSchema;
		}
		return paramsSchemaJson;
	}

	public static SolverTaskProfile fromPO(SolverTaskProfilePO po) {
		SolverTaskProfile profile = new SolverTaskProfile();
		profile.setId(po.getId());
		profile.setSolverId(po.getSolverId());
		profile.setProfileCode(po.getProfileCode());
		profile.setTaskType(po.getTaskType());
		profile.setProfileName(po.getProfileName());
		profile.setCommandTemplate(po.getCommandTemplate());
		profile.setParamsSchemaJson(po.getParamsSchemaJson());
		profile.setParserName(po.getParserName());
		profile.setTimeoutSeconds(po.getTimeoutSeconds());
		profile.setDescription(po.getDescription());
		profile.setEnabled(po.getEnabled());
		profile.setCreatedAt(po.getCreatedAt());
		profile.setUpdatedAt(po.getUpdatedAt());
		return profile;
	}

	public static SolverTaskProfilePO toPO(SolverTaskProfile profile) {
		SolverTaskProfilePO po = new SolverTaskProfilePO();
		po.setId(profile.getId());
		po.setSolverId(profile.getSolverId());
		po.setProfileCode(profile.getProfileCode());
		po.setTaskType(profile.getTaskType());
		po.setProfileName(profile.getProfileName());
		po.setCommandTemplate(profile.getCommandTemplate());
		po.setParamsSchemaJson(profile.getParamsSchemaJson());
		po.setParserName(profile.getParserName());
		po.setTimeoutSeconds(profile.getTimeoutSeconds());
		po.setDescription(profile.getDescription());
		po.setEnabled(profile.getEnabled());
		return po;
	}
}

