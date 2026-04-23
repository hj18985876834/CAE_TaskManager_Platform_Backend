package com.example.cae.solver.infrastructure.persistence.entity;

public class SolverProfileFileRulePO {
	private Long id;
	private Long profileId;
	private String fileKey;
	private String pathPattern;
	private String fileNamePattern;
	private String fileType;
	private Integer requiredFlag;
	private Integer sortOrder;
	private String ruleJson;
	private String description;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public String getPathPattern() {
		return pathPattern;
	}

	public void setPathPattern(String pathPattern) {
		this.pathPattern = pathPattern;
	}

	public String getFileNamePattern() {
		return fileNamePattern;
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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
