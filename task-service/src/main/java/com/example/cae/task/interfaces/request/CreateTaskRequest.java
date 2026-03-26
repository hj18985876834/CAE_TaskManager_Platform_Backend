package com.example.cae.task.interfaces.request;

import java.util.Map;

public class CreateTaskRequest {
	private String taskName;
	private Long solverId;
	private Long profileId;
	private String taskType;
	private Map<String, Object> paramsJson;

	public String getTaskName() {
		return taskName;
	}

	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}

	public Long getSolverId() {
		return solverId;
	}

	public void setSolverId(Long solverId) {
		this.solverId = solverId;
	}

	public Long getProfileId() {
		return profileId;
	}

	public void setProfileId(Long profileId) {
		this.profileId = profileId;
	}

	public String getTaskType() {
		return taskType;
	}

	public void setTaskType(String taskType) {
		this.taskType = taskType;
	}

	public Map<String, Object> getParamsJson() {
		return paramsJson;
	}

	public void setParamsJson(Map<String, Object> paramsJson) {
		this.paramsJson = paramsJson;
	}
}

