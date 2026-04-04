package com.example.cae.solver.interfaces.response;

import java.util.List;

public class UploadSpecResponse {
	private Long profileId;
	private String profileCode;
	private String taskType;
	private String profileName;
	private String uploadMode;
	private ArchiveRule archiveRule;
	private String paramsSchema;
	private String paramsSchemaJson;
	private Integer timeoutSeconds;
	private String description;
	private List<FileRuleResponse> fileRules;
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

	public String getUploadMode() {
		return uploadMode;
	}

	public void setUploadMode(String uploadMode) {
		this.uploadMode = uploadMode;
	}

	public ArchiveRule getArchiveRule() {
		return archiveRule;
	}

	public void setArchiveRule(ArchiveRule archiveRule) {
		this.archiveRule = archiveRule;
	}

	public Integer getTimeoutSeconds() {
		return timeoutSeconds;
	}

	public void setTimeoutSeconds(Integer timeoutSeconds) {
		this.timeoutSeconds = timeoutSeconds;
	}

	public String getParamsSchema() {
		return paramsSchema;
	}

	public void setParamsSchema(String paramsSchema) {
		this.paramsSchema = paramsSchema;
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

	public List<FileRuleResponse> getFileRules() {
		return fileRules;
	}

	public void setFileRules(List<FileRuleResponse> fileRules) {
		this.fileRules = fileRules;
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

	public static class ArchiveRule {
		private String fileKey;
		private java.util.List<String> allowSuffix;
		private Integer maxSizeMb;

		public String getFileKey() {
			return fileKey;
		}

		public void setFileKey(String fileKey) {
			this.fileKey = fileKey;
		}

		public java.util.List<String> getAllowSuffix() {
			return allowSuffix;
		}

		public void setAllowSuffix(java.util.List<String> allowSuffix) {
			this.allowSuffix = allowSuffix;
		}

		public Integer getMaxSizeMb() {
			return maxSizeMb;
		}

		public void setMaxSizeMb(Integer maxSizeMb) {
			this.maxSizeMb = maxSizeMb;
		}
	}
}
