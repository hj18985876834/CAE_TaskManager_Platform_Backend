package com.example.cae.task.interfaces.request;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.Map;

public class CreateTaskRequest {
	private String taskName;
	private Long solverId;
	private Long profileId;
	private String taskType;
	@JsonAlias("paramsJson")
	private Map<String, Object> params;

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

	public Map<String, Object> getParams() {
		return params;
	}

	public void setParams(Map<String, Object> params) {
		this.params = params;
	}
}
