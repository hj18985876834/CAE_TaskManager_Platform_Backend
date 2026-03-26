package com.example.cae.task.infrastructure.persistence.repository;

import com.example.cae.task.application.assembler.TaskLogAssembler;
import com.example.cae.task.domain.model.TaskLogChunk;
import com.example.cae.task.domain.repository.TaskLogRepository;
import com.example.cae.task.infrastructure.persistence.mapper.TaskLogChunkMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TaskLogRepositoryImpl implements TaskLogRepository {
	private final TaskLogChunkMapper taskLogChunkMapper;
	private final TaskLogAssembler taskLogAssembler;

	public TaskLogRepositoryImpl(TaskLogChunkMapper taskLogChunkMapper, TaskLogAssembler taskLogAssembler) {
		this.taskLogChunkMapper = taskLogChunkMapper;
		this.taskLogAssembler = taskLogAssembler;
	}

	@Override
	public void save(TaskLogChunk chunk) {
		taskLogChunkMapper.insert(taskLogAssembler.toPO(chunk));
	}

	@Override
	public List<TaskLogChunk> listByTaskIdAndSeq(Long taskId, Integer fromSeq, Integer pageSize) {
		return taskLogChunkMapper.selectByTaskIdAndSeq(taskId, fromSeq, pageSize).stream().map(taskLogAssembler::fromPO).toList();
	}
}

