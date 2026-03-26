package com.example.cae.task.infrastructure.support;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Component
public class LogChunkAppender {
	public void append(String logFilePath, Integer seqNo, String content) {
		try {
			Path path = Path.of(logFilePath);
			Files.createDirectories(path.getParent());
			String line = "[" + seqNo + "] " + content + System.lineSeparator();
			Files.writeString(path, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException ex) {
			throw new IllegalStateException("append log failed", ex);
		}
	}
}

