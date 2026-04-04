package com.example.cae.solver.interfaces.response;

public class FileRuleCreateResponse {
	private Long ruleId;
	private Long profileId;
	private String fileKey;
	private String pathPattern;
	private String fileNamePattern;
	private String fileType;
	private Integer requiredFlag;
	private Integer sortOrder;
	private String ruleJson;

	public Long getRuleId() {
		return ruleId;
	}

	public void setRuleId(Long ruleId) {
		this.ruleId = ruleId;
	}

	public Long getProfileId() {
		return profileId;
	}

	public void setProfileId(Long profileId) {
		this.profileId = profileId;
	}

	public String getFileKey() {
		return fileKey;
	}

	public void setFileKey(String fileKey) {
		this.fileKey = fileKey;
	}

	public String getFileNamePattern() {
		return fileNamePattern;
	}

	public String getPathPattern() {
		return pathPattern;
	}

	public void setPathPattern(String pathPattern) {
		this.pathPattern = pathPattern;
	}

	public void setFileNamePattern(String fileNamePattern) {
		this.fileNamePattern = fileNamePattern;
	}

	public String getFileType() {
		return fileType;
	}

	public void setFileType(String fileType) {
		this.fileType = fileType;
	}

	public Integer getRequiredFlag() {
		return requiredFlag;
	}

	public void setRequiredFlag(Integer requiredFlag) {
		this.requiredFlag = requiredFlag;
	}

	public Integer getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(Integer sortOrder) {
		this.sortOrder = sortOrder;
	}

	public String getRuleJson() {
		return ruleJson;
	}

	public void setRuleJson(String ruleJson) {
		this.ruleJson = ruleJson;
	}
}
