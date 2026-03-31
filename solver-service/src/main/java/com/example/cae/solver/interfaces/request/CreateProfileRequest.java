package com.example.cae.solver.interfaces.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class CreateProfileRequest {
	@NotNull(message = "solverId不能为空")
	@Positive(message = "solverId必须大于0")
	private Long solverId;
	@NotBlank(message = "profileCode不能为空")
	@Size(max = 64, message = "profileCode长度不能超过64")
	private String profileCode;
	@NotBlank(message = "taskType不能为空")
	@Size(max = 64, message = "taskType长度不能超过64")
	private String taskType;
	@NotBlank(message = "profileName不能为空")
	@Size(max = 128, message = "profileName长度不能超过128")
	private String profileName;
	@NotBlank(message = "commandTemplate不能为空")
	private String commandTemplate;
	private String paramsSchema;
	private String paramsSchemaJson;
	@NotBlank(message = "parserName不能为空")
	@Size(max = 128, message = "parserName长度不能超过128")
	private String parserName;
	@NotNull(message = "timeoutSeconds不能为空")
	@Positive(message = "timeoutSeconds必须大于0")
	private Integer timeoutSeconds;
	@Min(value = 0, message = "enabled只能为0或1")
	@Max(value = 1, message = "enabled只能为0或1")
	private Integer enabled;
	@Size(max = 255, message = "description长度不能超过255")
	private String description;

	public Long getSolverId() {
		return solverId;
	}

	public void setSolverId(Long solverId) {
		this.solverId = solverId;
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

	public String getCommandTemplate() {
		return commandTemplate;
	}

	public void setCommandTemplate(String commandTemplate) {
		this.commandTemplate = commandTemplate;
	}

	public String getParamsSchema() {
		return paramsSchema;
	}

	public void setParamsSchema(String paramsSchema) {
		this.paramsSchema = paramsSchema;
	}

	public String getParserName() {
		return parserName;
	}

	public void setParserName(String parserName) {
		this.parserName = parserName;
	}

	public String getParamsSchemaJson() {
		return paramsSchemaJson;
	}

	public void setParamsSchemaJson(String paramsSchemaJson) {
		this.paramsSchemaJson = paramsSchemaJson;
	}

	public Integer getTimeoutSeconds() {
		return timeoutSeconds;
	}

	public void setTimeoutSeconds(Integer timeoutSeconds) {
		this.timeoutSeconds = timeoutSeconds;
	}

	public Integer getEnabled() {
		return enabled;
	}

	public void setEnabled(Integer enabled) {
		this.enabled = enabled;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
