package com.example.cae.nodeagent.infrastructure.process;

public class ProcessTimeoutException extends RuntimeException {
	public ProcessTimeoutException(String message) {
		super(message);
	}
}
