package com.example.cae.solver.interfaces.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class ProfilePageQueryRequest {
	@Min(value = 1, message = "pageNum必须大于等于1")
	private Integer pageNum;
	@Min(value = 1, message = "pageSize必须大于等于1")
	@Max(value = 200, message = "pageSize不能超过200")
	private Integer pageSize;
	@Positive(message = "solverId必须大于0")
	private Long solverId;
	@Size(max = 50, message = "taskType长度不能超过50")
	private String taskType;
	@Size(max = 50, message = "profileCode长度不能超过50")
	private String profileCode;
	@Min(value = 0, message = "enabled只能为0或1")
	@Max(value = 1, message = "enabled只能为0或1")
	private Integer enabled;

	public Integer getPageNum() {
		return pageNum;
	}

	public void setPageNum(Integer pageNum) {
		this.pageNum = pageNum;
	}

	public Integer getPageSize() {
		return pageSize;
	}

	public void setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
	}

	public Long getSolverId() {
		return solverId;
	}

	public void setSolverId(Long solverId) {
		this.solverId = solverId;
	}

	public String getTaskType() {
		return taskType;
	}

	public void setTaskType(String taskType) {
		this.taskType = taskType;
	}

	public String getProfileCode() {
		return profileCode;
	}

	public void setProfileCode(String profileCode) {
		this.profileCode = profileCode;
	}

	public Integer getEnabled() {
		return enabled;
	}

	public void setEnabled(Integer enabled) {
		this.enabled = enabled;
	}
}

