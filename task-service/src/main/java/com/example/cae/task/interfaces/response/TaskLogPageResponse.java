package com.example.cae.task.interfaces.response;

import java.util.List;

public class TaskLogPageResponse {
	private Long taskId;
	private Integer nextSeq;
	private List<TaskLogResponse> records;

	public Long getTaskId() {
		return taskId;
	}

	public void setTaskId(Long taskId) {
		this.taskId = taskId;
	}

	public Integer getNextSeq() {
		return nextSeq;
	}

	public void setNextSeq(Integer nextSeq) {
		this.nextSeq = nextSeq;
	}

	public List<TaskLogResponse> getRecords() {
		return records;
	}

	public void setRecords(List<TaskLogResponse> records) {
		this.records = records;
	}
}
