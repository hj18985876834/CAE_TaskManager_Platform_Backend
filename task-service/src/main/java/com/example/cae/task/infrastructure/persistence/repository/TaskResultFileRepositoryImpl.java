package com.example.cae.task.infrastructure.persistence.repository;

import com.example.cae.task.domain.model.TaskResultFile;
import com.example.cae.task.domain.repository.TaskResultFileRepository;
import com.example.cae.task.infrastructure.persistence.entity.TaskResultFilePO;
import com.example.cae.task.infrastructure.persistence.mapper.TaskResultFileMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TaskResultFileRepositoryImpl implements TaskResultFileRepository {
	private final TaskResultFileMapper taskResultFileMapper;

	public TaskResultFileRepositoryImpl(TaskResultFileMapper taskResultFileMapper) {
		this.taskResultFileMapper = taskResultFileMapper;
	}

	@Override
	public void save(TaskResultFile file) {
		TaskResultFilePO po = new TaskResultFilePO();
		po.setTaskId(file.getTaskId());
		po.setFileType(file.getFileType());
		po.setFileName(file.getFileName());
		po.setStoragePath(file.getStoragePath());
		po.setFileSize(file.getFileSize());
		taskResultFileMapper.insert(po);
	}

	@Override
	public List<TaskResultFile> listByTaskId(Long taskId) {
		return taskResultFileMapper.selectByTaskId(taskId).stream().map(this::toDomain).toList();
	}

	@Override
	public Optional<TaskResultFile> findById(Long fileId) {
		return Optional.ofNullable(taskResultFileMapper.selectById(fileId)).map(this::toDomain);
	}

	private TaskResultFile toDomain(TaskResultFilePO po) {
		TaskResultFile file = new TaskResultFile();
		file.setId(po.getId());
		file.setTaskId(po.getTaskId());
		file.setFileType(po.getFileType());
		file.setFileName(po.getFileName());
		file.setStoragePath(po.getStoragePath());
		file.setFileSize(po.getFileSize());
		file.setCreatedAt(po.getCreatedAt());
		return file;
	}
}

