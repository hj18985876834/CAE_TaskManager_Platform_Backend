package com.example.cae.task.infrastructure.persistence.entity;

import java.time.LocalDateTime;

public class TaskLogChunkPO {
	private Long id;
	private Long taskId;
	private Integer seqNo;
	private String logContent;
	private LocalDateTime createdAt;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public Long getTaskId() { return taskId; }
	public void setTaskId(Long taskId) { this.taskId = taskId; }
	public Integer getSeqNo() { return seqNo; }
	public void setSeqNo(Integer seqNo) { this.seqNo = seqNo; }
	public String getLogContent() { return logContent; }
	public void setLogContent(String logContent) { this.logContent = logContent; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

