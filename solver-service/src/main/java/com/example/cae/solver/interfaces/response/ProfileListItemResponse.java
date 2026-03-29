package com.example.cae.solver.interfaces.response;

public class ProfileListItemResponse {
	private Long id;
	private Long profileId;
	private Long solverId;
	private String profileCode;
	private String taskType;
	private String profileName;
	private String paramsSchema;
	private String commandTemplate;
	private String parserName;
	private Integer timeoutSeconds;
	private String description;
	private Integer enabled;
       public String getCommandTemplate() {
	       return commandTemplate;
       }

       public void setCommandTemplate(String commandTemplate) {
	       this.commandTemplate = commandTemplate;
       }

       public String getParserName() {
	       return parserName;
       }

       public void setParserName(String parserName) {
	       this.parserName = parserName;
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

	public String getParamsSchema() {
		return paramsSchema;
	}

	public void setParamsSchema(String paramsSchema) {
		this.paramsSchema = paramsSchema;
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

	public Integer getEnabled() {
		return enabled;
	}

	public void setEnabled(Integer enabled) {
		this.enabled = enabled;
	}
}
