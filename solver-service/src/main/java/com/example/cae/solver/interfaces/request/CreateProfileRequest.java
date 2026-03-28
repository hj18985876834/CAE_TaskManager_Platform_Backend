package com.example.cae.solver.interfaces.request;

public class CreateProfileRequest {
	private Long solverId;
	private String profileCode;
	private String taskType;
	private String profileName;
	private String commandTemplate;
	private String paramsSchema;
	private String paramsSchemaJson;
	private String parserName;
	private Integer timeoutSeconds;
	private Integer enabled;
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

