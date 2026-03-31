package com.example.cae.nodeagent.infrastructure.process;

public class ProcessCanceledException extends RuntimeException {
	public ProcessCanceledException(String message) {
		super(message);
	}
}
