package com.example.cae.solver.interfaces.response;

import java.util.List;

public class UploadSpecResponse {
	private Long profileId;
	private String profileCode;
	private String taskType;
	private String profileName;
	private String paramsSchemaJson;
	private Integer timeoutSeconds;
	private String description;
	private List<FileRuleResponse> requiredFiles;
	private List<FileRuleResponse> optionalFiles;

	public Long getProfileId() {
		return profileId;
	}

	public void setProfileId(Long profileId) {
		this.profileId = profileId;
	}

	public String getProfileCode() {
		return profileCode;
	}

	public void setProfileCode(String profileCode) {
		this.profileCode = profileCode;
	}

	public String getTaskType() {
		return taskType;
	}

	public void setTaskType(String taskType) {
		this.taskType = taskType;
	}

	public String getProfileName() {
		return profileName;
	}

	public void setProfileName(String profileName) {
		this.profileName = profileName;
	}

	public Integer getTimeoutSeconds() {
		return timeoutSeconds;
	}

	public void setTimeoutSeconds(Integer timeoutSeconds) {
		this.timeoutSeconds = timeoutSeconds;
	}

	public String getParamsSchemaJson() {
		return paramsSchemaJson;
	}

	public void setParamsSchemaJson(String paramsSchemaJson) {
		this.paramsSchemaJson = paramsSchemaJson;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<FileRuleResponse> getRequiredFiles() {
		return requiredFiles;
	}

	public void setRequiredFiles(List<FileRuleResponse> requiredFiles) {
		this.requiredFiles = requiredFiles;
	}

	public List<FileRuleResponse> getOptionalFiles() {
		return optionalFiles;
	}

	public void setOptionalFiles(List<FileRuleResponse> optionalFiles) {
		this.optionalFiles = optionalFiles;
	}
}
