package com.example.cae.task.infrastructure.storage;

import com.example.cae.common.enums.FileRoleEnum;
import com.example.cae.task.domain.model.TaskFile;
import com.example.cae.task.infrastructure.support.TaskPathResolver;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class LocalTaskFileStorageService implements TaskFileStorageService {
	private final TaskPathResolver taskPathResolver;

	public LocalTaskFileStorageService(TaskPathResolver taskPathResolver) {
		this.taskPathResolver = taskPathResolver;
	}

	@Override
	public TaskFile saveInputFile(Long taskId, MultipartFile file) {
		String dir = taskPathResolver.resolveInputDir(taskId);
		String fileName = file.getOriginalFilename() == null ? "unknown.bin" : file.getOriginalFilename();
		Path path = Path.of(dir, fileName);
		try {
			Files.createDirectories(path.getParent());
			file.transferTo(path);
		} catch (IOException ex) {
			throw new IllegalStateException("save input file failed", ex);
		}

		TaskFile taskFile = new TaskFile();
		taskFile.setTaskId(taskId);
		taskFile.setFileRole(FileRoleEnum.INPUT.name());
		taskFile.setOriginName(fileName);
		taskFile.setStoragePath(path.toString().replace("\\", "/"));
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

	private String extractSuffix(String fileName) {
		int idx = fileName.lastIndexOf('.');
		return idx < 0 ? "" : fileName.substring(idx + 1);
	}
}

