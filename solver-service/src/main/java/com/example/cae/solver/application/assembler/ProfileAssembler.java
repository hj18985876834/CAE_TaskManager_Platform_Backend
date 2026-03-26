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
		profile.setParserName(request.getParserName());
		profile.setTimeoutSeconds(request.getTimeoutSeconds());
		return profile;
	}

	public static ProfileListItemResponse toListItemResponse(SolverTaskProfile profile) {
		ProfileListItemResponse response = new ProfileListItemResponse();
		response.setProfileId(profile.getId());
		response.setSolverId(profile.getSolverId());
		response.setProfileCode(profile.getProfileCode());
		response.setTaskType(profile.getTaskType());
		response.setProfileName(profile.getProfileName());
		response.setTimeoutSeconds(profile.getTimeoutSeconds());
		response.setEnabled(profile.getEnabled());
		return response;
	}

	public static ProfileDetailResponse toDetailResponse(SolverTaskProfile profile) {
		ProfileDetailResponse response = new ProfileDetailResponse();
		response.setProfileId(profile.getId());
		response.setSolverId(profile.getSolverId());
		response.setProfileCode(profile.getProfileCode());
		response.setTaskType(profile.getTaskType());
		response.setProfileName(profile.getProfileName());
		response.setCommandTemplate(profile.getCommandTemplate());
		response.setParserName(profile.getParserName());
		response.setTimeoutSeconds(profile.getTimeoutSeconds());
		response.setEnabled(profile.getEnabled());
		return response;
	}

	public static SolverTaskProfile fromPO(SolverTaskProfilePO po) {
		SolverTaskProfile profile = new SolverTaskProfile();
		profile.setId(po.getId());
		profile.setSolverId(po.getSolverId());
		profile.setProfileCode(po.getProfileCode());
		profile.setTaskType(po.getTaskType());
		profile.setProfileName(po.getProfileName());
		profile.setCommandTemplate(po.getCommandTemplate());
		profile.setParserName(po.getParserName());
		profile.setTimeoutSeconds(po.getTimeoutSeconds());
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
		po.setParserName(profile.getParserName());
		po.setTimeoutSeconds(profile.getTimeoutSeconds());
		po.setEnabled(profile.getEnabled());
		return po;
	}
}

