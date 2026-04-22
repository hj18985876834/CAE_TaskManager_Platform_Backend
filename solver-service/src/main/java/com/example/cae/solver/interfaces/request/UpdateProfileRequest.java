package com.example.cae.solver.interfaces.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {
	@NotBlank(message = "taskType不能为空")
	@Size(max = 50, message = "taskType长度不能超过50")
	private String taskType;
	@NotBlank(message = "profileName不能为空")
	@Size(max = 100, message = "profileName长度不能超过100")
	private String profileName;
	@NotBlank(message = "uploadMode不能为空")
	@Size(max = 32, message = "uploadMode长度不能超过32")
	private String uploadMode;
	@NotBlank(message = "commandTemplate不能为空")
	@Size(max = 255, message = "commandTemplate长度不能超过255")
	private String commandTemplate;
	private String paramsSchema;
	private String paramsSchemaJson;
	@NotBlank(message = "parserName不能为空")
	@Size(max = 100, message = "parserName长度不能超过100")
	private String parserName;
	@NotNull(message = "timeoutSeconds不能为空")
	@Positive(message = "timeoutSeconds必须大于0")
	private Integer timeoutSeconds;
	@Size(max = 255, message = "description长度不能超过255")
	private String description;

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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
