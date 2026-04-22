package com.example.cae.task.interfaces.response;

public class AdminTaskListItemResponse extends TaskListItemResponse {
	private Long userId;
	private String username;
	private String failType;

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getFailType() {
		return failType;
	}

	public void setFailType(String failType) {
		this.failType = failType;
	}
}
