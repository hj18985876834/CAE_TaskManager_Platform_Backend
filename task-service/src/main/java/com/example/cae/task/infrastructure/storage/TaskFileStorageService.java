package com.example.cae.task.infrastructure.storage;

import com.example.cae.task.domain.model.TaskFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface TaskFileStorageService {
	TaskFile saveInputFile(Long taskId, MultipartFile file, String fileKey, String fileRole);

	InputStream openFile(String storagePath);

	void deleteFile(String storagePath);
}
