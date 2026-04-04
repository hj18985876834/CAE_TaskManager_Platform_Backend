package com.example.cae.task.infrastructure.persistence.repository;

import com.example.cae.task.domain.model.TaskFile;
import com.example.cae.task.domain.repository.TaskFileRepository;
import com.example.cae.task.infrastructure.persistence.entity.TaskFilePO;
import com.example.cae.task.infrastructure.persistence.mapper.TaskFileMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TaskFileRepositoryImpl implements TaskFileRepository {
	private final TaskFileMapper taskFileMapper;

	public TaskFileRepositoryImpl(TaskFileMapper taskFileMapper) {
		this.taskFileMapper = taskFileMapper;
	}

	@Override
	public void save(TaskFile file) {
		TaskFilePO po = toPO(file);
		taskFileMapper.insert(po);
		file.setId(po.getId());
	}

	@Override
	public void saveBatch(List<TaskFile> files) {
		for (TaskFile file : files) {
			TaskFilePO po = toPO(file);
			taskFileMapper.insert(po);
			file.setId(po.getId());
		}
	}

	@Override
	public void update(TaskFile file) {
		TaskFilePO po = toPO(file);
		po.setId(file.getId());
		taskFileMapper.updateById(po);
	}

	@Override
	public List<TaskFile> listByTaskId(Long taskId) {
		return taskFileMapper.selectByTaskId(taskId).stream().map(this::toDomain).toList();
	}

	@Override
	public void deleteById(Long id) {
		taskFileMapper.deleteById(id);
	}

	private TaskFile toDomain(TaskFilePO po) {
		TaskFile file = new TaskFile();
		file.setId(po.getId());
		file.setTaskId(po.getTaskId());
		file.setFileRole(po.getFileRole());
		file.setFileKey(po.getFileKey());
		file.setOriginName(po.getOriginName());
		file.setStoragePath(po.getStoragePath());
		file.setUnpackDir(po.getUnpackDir());
		file.setRelativePath(po.getRelativePath());
		file.setArchiveFlag(po.getArchiveFlag());
		file.setFileSize(po.getFileSize());
		file.setFileSuffix(po.getFileSuffix());
		file.setChecksum(po.getChecksum());
		file.setCreatedAt(po.getCreatedAt());
		return file;
	}

	private TaskFilePO toPO(TaskFile file) {
		TaskFilePO po = new TaskFilePO();
		po.setTaskId(file.getTaskId());
		po.setFileRole(file.getFileRole());
		po.setFileKey(file.getFileKey());
		po.setOriginName(file.getOriginName());
		po.setStoragePath(file.getStoragePath());
		po.setUnpackDir(file.getUnpackDir());
		po.setRelativePath(file.getRelativePath());
		po.setArchiveFlag(file.getArchiveFlag());
		po.setFileSize(file.getFileSize());
		po.setFileSuffix(file.getFileSuffix());
		po.setChecksum(file.getChecksum());
		return po;
	}
}
