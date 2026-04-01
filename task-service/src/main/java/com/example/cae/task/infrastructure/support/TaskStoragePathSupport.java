package com.example.cae.task.infrastructure.support;

import com.example.cae.task.config.TaskStorageProperties;
import org.springframework.stereotype.Component;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

@Component
public class TaskStoragePathSupport {
	private final TaskStorageProperties taskStorageProperties;

	public TaskStoragePathSupport(TaskStorageProperties taskStorageProperties) {
		this.taskStorageProperties = taskStorageProperties;
	}

	public String toStoredTaskPath(String path) {
		return normalizeForStorage(path, taskStorageProperties.getTaskRoot());
	}

	public String toStoredResultPath(String path) {
		return normalizeForStorage(path, taskStorageProperties.getResultRoot());
	}

	public String toAbsoluteTaskPath(String path) {
		return resolveAbsolutePath(path, taskStorageProperties.getTaskRoot());
	}

	public String toAbsoluteResultPath(String path) {
		return resolveAbsolutePath(path, taskStorageProperties.getResultRoot());
	}

	public String toDisplayTaskPath(String path) {
		return toDisplayPath(path, taskStorageProperties.getTaskRoot());
	}

	public String toDisplayResultPath(String path) {
		return toDisplayPath(path, taskStorageProperties.getResultRoot());
	}

	private String normalizeForStorage(String path, String root) {
		if (path == null || path.isBlank()) {
			return path;
		}
		String normalizedPath = normalize(path);
		String normalizedRoot = normalize(root);
		if (!isAbsolutePath(normalizedPath)) {
			return normalizedPath;
		}
		if (normalizedRoot != null && normalizedPath.equals(normalizedRoot)) {
			return "";
		}
		if (normalizedRoot != null && normalizedPath.startsWith(normalizedRoot + "/")) {
			return normalizedPath.substring(normalizedRoot.length() + 1);
		}
		return normalizedPath;
	}

	private String resolveAbsolutePath(String path, String root) {
		if (path == null || path.isBlank()) {
			return path;
		}
		String normalizedPath = normalize(path);
		if (isAbsolutePath(normalizedPath)) {
			return normalizedPath;
		}
		return normalize(Path.of(root, normalizedPath).toString());
	}

	private String toDisplayPath(String path, String root) {
		if (path == null || path.isBlank()) {
			return path;
		}
		String stored = normalizeForStorage(path, root);
		if (!isAbsolutePath(stored)) {
			return stored;
		}
		try {
			Path fileName = Path.of(stored).getFileName();
			return fileName == null ? stored : fileName.toString().replace("\\", "/");
		} catch (InvalidPathException ex) {
			return stored;
		}
	}

	private String normalize(String path) {
		try {
			return Path.of(path).normalize().toString().replace("\\", "/");
		} catch (InvalidPathException ex) {
			return path.replace("\\", "/");
		}
	}

	private boolean isAbsolutePath(String path) {
		if (path == null || path.isBlank()) {
			return false;
		}
		return path.startsWith("/")
				|| path.startsWith("\\\\")
				|| path.matches("^[A-Za-z]:[\\\\/].*");
	}
}
