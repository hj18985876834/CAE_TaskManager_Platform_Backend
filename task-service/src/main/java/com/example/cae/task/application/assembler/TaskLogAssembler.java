package com.example.cae.task.application.assembler;

import com.example.cae.task.domain.model.TaskLogChunk;
import com.example.cae.task.infrastructure.persistence.entity.TaskLogChunkPO;
import com.example.cae.task.interfaces.response.TaskLogResponse;
import org.springframework.stereotype.Component;

@Component
public class TaskLogAssembler {
	public TaskLogResponse toResponse(TaskLogChunk chunk) {
		TaskLogResponse response = new TaskLogResponse();
		response.setSeqNo(chunk.getSeqNo());
		response.setLogContent(chunk.getLogContent());
		response.setCreatedAt(chunk.getCreatedAt());
		return response;
	}

	public TaskLogChunkPO toPO(TaskLogChunk chunk) {
		TaskLogChunkPO po = new TaskLogChunkPO();
		po.setId(chunk.getId());
		po.setTaskId(chunk.getTaskId());
		po.setSeqNo(chunk.getSeqNo());
		po.setLogContent(chunk.getLogContent());
		return po;
	}

	public TaskLogChunk fromPO(TaskLogChunkPO po) {
		TaskLogChunk chunk = new TaskLogChunk();
		chunk.setId(po.getId());
		chunk.setTaskId(po.getTaskId());
		chunk.setSeqNo(po.getSeqNo());
		chunk.setLogContent(po.getLogContent());
		chunk.setCreatedAt(po.getCreatedAt());
		return chunk;
	}
}
