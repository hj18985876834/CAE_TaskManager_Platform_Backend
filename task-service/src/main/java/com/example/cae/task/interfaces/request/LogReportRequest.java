package com.example.cae.task.interfaces.request;

public class LogReportRequest {
	private Long nodeId;
	private Integer seqNo;
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

	public String getContent() {
		return logContent;
	}

	public void setContent(String content) {
		this.logContent = content;
	}
}

