package com.example.cae.task.infrastructure.storage;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.enums.FileRoleEnum;
import com.example.cae.common.exception.BizException;
import com.example.cae.task.domain.model.TaskFile;
import com.example.cae.task.infrastructure.support.TaskPathResolver;
import com.example.cae.task.infrastructure.support.TaskStoragePathSupport;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

@Service
public class LocalTaskFileStorageService implements TaskFileStorageService {
	private final TaskPathResolver taskPathResolver;
	private final TaskStoragePathSupport taskStoragePathSupport;

	public LocalTaskFileStorageService(TaskPathResolver taskPathResolver, TaskStoragePathSupport taskStoragePathSupport) {
		this.taskPathResolver = taskPathResolver;
		this.taskStoragePathSupport = taskStoragePathSupport;
	}

	@Override
	public TaskFile saveInputFile(Long taskId, MultipartFile file, String fileKey, String fileRole) {
		if (file == null || file.isEmpty()) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "file is required");
		}
		String normalizedFileRole = normalizeFileRole(fileRole);
		String fileName = file.getOriginalFilename() == null ? "unknown.bin" : file.getOriginalFilename();
		String normalizedFileKey = normalizeFileKey(fileKey, fileName);
		String dir = taskPathResolver.resolveInputDir(taskId);
		Path path;
		try {
			path = Path.of(dir, fileName);
		} catch (InvalidPathException ex) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "invalid file name");
		}
		try {
			Files.createDirectories(path.getParent());
			file.transferTo(path);
		} catch (IOException ex) {
			throw new IllegalStateException("save input file failed", ex);
		}

		TaskFile taskFile = new TaskFile();
		taskFile.setTaskId(taskId);
		taskFile.setFileRole(normalizedFileRole);
		taskFile.setFileKey(normalizedFileKey);
		taskFile.setOriginName(fileName);
		taskFile.setStoragePath(taskStoragePathSupport.toStoredTaskPath(path.toString()));
		taskFile.setFileSize(file.getSize());
		taskFile.setFileSuffix(extractSuffix(fileName));
		return taskFile;
	}

	@Override
	public InputStream openFile(String storagePath) {
		try {
			return Files.newInputStream(Path.of(storagePath));
		} catch (IOException ex) {
			throw new IllegalStateException("open file failed", ex);
		}
	}

	@Override
	public void deleteFile(String storagePath) {
		try {
			Files.deleteIfExists(Path.of(storagePath));
		} catch (IOException ex) {
			throw new IllegalStateException("delete file failed", ex);
		}
	}

	@Override
	public void deleteTaskArtifacts(Long taskId) {
		if (taskId == null) {
			return;
		}
		Path taskRoot = Path.of(taskPathResolver.resolveTaskRoot(taskId));
		if (!Files.exists(taskRoot)) {
			return;
		}
		try (var stream = Files.walk(taskRoot)) {
			stream.sorted(Comparator.reverseOrder()).forEach(path -> {
				try {
					Files.deleteIfExists(path);
				} catch (IOException ex) {
					throw new IllegalStateException("delete task artifacts failed", ex);
				}
			});
		} catch (IOException ex) {
			throw new IllegalStateException("delete task artifacts failed", ex);
		}
	}

	@Override
	public void deleteTaskRuntimeArtifacts(Long taskId) {
		if (taskId == null) {
			return;
		}
		deleteDirectory(Path.of(taskPathResolver.resolveLogDir(taskId)), "delete task runtime artifacts failed");
		deleteDirectory(Path.of(taskPathResolver.resolveResultDir(taskId)), "delete task runtime artifacts failed");
	}

	@Override
	public void deleteTaskPreparedArtifacts(Long taskId) {
		if (taskId == null) {
			return;
		}
		deleteDirectory(Path.of(taskPathResolver.resolveWorkDir(taskId)), "delete task prepared artifacts failed");
		deleteTaskRuntimeArtifacts(taskId);
	}

	private String extractSuffix(String fileName) {
		int idx = fileName.lastIndexOf('.');
		return idx < 0 ? "" : fileName.substring(idx + 1);
	}

	private String normalizeFileRole(String fileRole) {
		if (fileRole == null || fileRole.isBlank()) {
			return FileRoleEnum.INPUT.name();
		}
		try {
			return FileRoleEnum.valueOf(fileRole.trim().toUpperCase()).name();
		} catch (IllegalArgumentException ex) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "unsupported fileRole: " + fileRole);
		}
	}

	private String normalizeFileKey(String fileKey, String fileName) {
		if (fileKey != null && !fileKey.isBlank()) {
			return fileKey.trim();
		}
		int idx = fileName.lastIndexOf('.');
		return idx > 0 ? fileName.substring(0, idx) : fileName;
	}

	private void deleteDirectory(Path dir, String errorMessage) {
		if (dir == null || !Files.exists(dir)) {
			return;
		}
		try (var stream = Files.walk(dir)) {
			stream.sorted(Comparator.reverseOrder()).forEach(path -> {
				try {
					Files.deleteIfExists(path);
				} catch (IOException ex) {
					throw new IllegalStateException(errorMessage, ex);
				}
			});
		} catch (IOException ex) {
			throw new IllegalStateException(errorMessage, ex);
		}
	}
}
