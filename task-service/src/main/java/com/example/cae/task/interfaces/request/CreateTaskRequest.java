package com.example.cae.task.interfaces.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Map;

public class CreateTaskRequest {
	@NotBlank(message = "taskName不能为空")
	@Size(max = 100, message = "taskName长度不能超过100")
	private String taskName;
	@NotNull(message = "solverId不能为空")
	@Positive(message = "solverId必须大于0")
	private Long solverId;
	@NotNull(message = "profileId不能为空")
	@Positive(message = "profileId必须大于0")
	private Long profileId;
	@NotBlank(message = "taskType不能为空")
	@Size(max = 50, message = "taskType长度不能超过50")
	private String taskType;
	@Min(value = 0, message = "priority不能小于0")
	private Integer priority;
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

	public Integer getPriority() {
		return priority;
	}

	public void setPriority(Integer priority) {
		this.priority = priority;
	}

	public Map<String, Object> getParams() {
		return params;
	}

	public void setParams(Map<String, Object> params) {
		this.params = params;
	}
}
