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
		return resolveAbsolutePath(path, taskStorageProperties.getTaskRoot());
	}

	public String toStoredResultPath(String path) {
		return resolveAbsolutePath(path, taskStorageProperties.getResultRoot());
	}

	public String toAbsoluteTaskPath(String path) {
		return resolveAbsolutePath(path, taskStorageProperties.getTaskRoot());
	}

	public String toAbsoluteResultPath(String path) {
		return resolveAbsolutePath(path, taskStorageProperties.getResultRoot());
	}

	public String toDisplayTaskPath(String path) {
		return resolveAbsolutePath(path, taskStorageProperties.getTaskRoot());
	}

	public String toDisplayResultPath(String path) {
		return resolveAbsolutePath(path, taskStorageProperties.getResultRoot());
	}

	private String resolveAbsolutePath(String path, String root) {
		if (path == null || path.isBlank()) {
			return path;
		}
		String normalizedPath = normalize(path);
		String normalizedRoot = normalizeRoot(root);
		if (!normalizedRoot.isBlank() && (normalizedPath.equals(normalizedRoot) || normalizedPath.startsWith(normalizedRoot + "/"))) {
			return normalizedPath;
		}
		if (isAbsolutePath(normalizedPath)) {
			return normalizedPath;
		}
		if (normalizedRoot.isBlank()) {
			return normalizedPath;
		}
		return normalize(Path.of(normalizedRoot, normalizedPath).toString());
	}

	private String normalize(String path) {
		try {
			return Path.of(path).normalize().toString().replace("\\", "/");
		} catch (InvalidPathException ex) {
			return path.replace("\\", "/");
		}
	}

	private String normalizeRoot(String root) {
		if (root == null || root.isBlank()) {
			return "";
		}
		try {
			return Path.of(root).toAbsolutePath().normalize().toString().replace("\\", "/");
		} catch (InvalidPathException ex) {
			return root.replace("\\", "/");
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
