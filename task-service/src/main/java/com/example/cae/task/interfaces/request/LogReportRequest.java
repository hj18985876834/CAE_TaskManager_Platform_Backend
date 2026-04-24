package com.example.cae.task.interfaces.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class LogReportRequest {
	@NotNull(message = "nodeId不能为空")
	@Positive(message = "nodeId必须大于0")
	private Long nodeId;
	@NotNull(message = "seqNo不能为空")
	@Min(value = 0, message = "seqNo不能小于0")
	private Integer seqNo;
	@NotNull(message = "logContent cannot be null")
	private String logContent;

	public Long getNodeId() {
		return nodeId;
	}

	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

	public Integer getSeqNo() {
		return seqNo;
	}

	public void setSeqNo(Integer seqNo) {
		this.seqNo = seqNo;
	}

	public String getLogContent() {
		return logContent;
	}

	public void setLogContent(String logContent) {
		this.logContent = logContent;
	}
}
