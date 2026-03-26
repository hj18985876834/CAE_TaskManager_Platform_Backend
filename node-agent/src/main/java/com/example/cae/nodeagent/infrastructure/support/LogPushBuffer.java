package com.example.cae.nodeagent.infrastructure.support;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class LogPushBuffer {
	private final List<String> buffer = new ArrayList<>();

	public synchronized void append(String line) {
		buffer.add(line);
	}

	public synchronized List<String> drain() {
		List<String> copy = new ArrayList<>(buffer);
		buffer.clear();
		return copy;
	}
}

