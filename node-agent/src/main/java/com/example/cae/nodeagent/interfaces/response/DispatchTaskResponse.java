package com.example.cae.nodeagent.interfaces.response;

public class DispatchTaskResponse {
	private Boolean accepted;
	private String message;

	public Boolean getAccepted() {
		return accepted;
	}

	public void setAccepted(Boolean accepted) {
		this.accepted = accepted;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}