package com.example.cae.solver.interfaces.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateFileRuleRequest {
	@NotBlank(message = "pathPattern不能为空")
	@Size(max = 255, message = "pathPattern长度不能超过255")
	private String pathPattern;
	@NotBlank(message = "fileNamePattern不能为空")
	@Size(max = 255, message = "fileNamePattern长度不能超过255")
	private String fileNamePattern;
	@NotBlank(message = "fileType不能为空")
	@Size(max = 32, message = "fileType长度不能超过32")
	private String fileType;
	@NotNull(message = "requiredFlag不能为空")
	@Min(value = 0, message = "requiredFlag只能为0或1")
	@Max(value = 1, message = "requiredFlag只能为0或1")
	private Integer requiredFlag;
	@NotNull(message = "sortOrder不能为空")
	@Min(value = 0, message = "sortOrder不能小于0")
	private Integer sortOrder;
	@Size(max = 255, message = "description长度不能超过255")
	private String description;
	@Size(max = 255, message = "remark长度不能超过255")
	private String remark;
	private String ruleJson;

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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public String getRuleJson() {
		return ruleJson;
	}

	public void setRuleJson(String ruleJson) {
		this.ruleJson = ruleJson;
	}
}

