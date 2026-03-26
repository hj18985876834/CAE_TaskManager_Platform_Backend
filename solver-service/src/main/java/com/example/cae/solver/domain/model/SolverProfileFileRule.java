package com.example.cae.solver.domain.model;

public class SolverProfileFileRule {
	private Long id;
	private Long profileId;
	private String fileKey;
	private String fileNamePattern;
	private String fileType;
	private Integer requiredFlag;
	private Integer sortOrder;
	private String remark;

	public boolean isRequired() {
		return this.requiredFlag != null && this.requiredFlag == 1;
	}

	public boolean matches(String fileName) {
		if (fileNamePattern == null || fileNamePattern.trim().isEmpty()) {
			return true;
		}
		String regex = fileNamePattern.replace(".", "\\.").replace("*", ".*");
		return fileName != null && fileName.matches(regex);
	}

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

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}
}

