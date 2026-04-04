package com.example.cae.task.interfaces.response;

public class TaskValidateResponse {
	private Long taskId;
	private Boolean valid;
	private String status;
	private java.util.List<ValidationIssue> issues;

	public Long getTaskId() {
		return taskId;
	}

	public void setTaskId(Long taskId) {
		this.taskId = taskId;
	}

	public Boolean getValid() {
		return valid;
	}

	public void setValid(Boolean valid) {
		this.valid = valid;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public java.util.List<ValidationIssue> getIssues() {
		return issues;
	}

	public void setIssues(java.util.List<ValidationIssue> issues) {
		this.issues = issues;
	}

	public static class ValidationIssue {
		private String ruleKey;
		private String path;
		private String errorCode;
		private String message;

		public String getRuleKey() {
			return ruleKey;
		}

		public void setRuleKey(String ruleKey) {
			this.ruleKey = ruleKey;
		}

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		public String getErrorCode() {
			return errorCode;
		}

		public void setErrorCode(String errorCode) {
			this.errorCode = errorCode;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
	}
}
