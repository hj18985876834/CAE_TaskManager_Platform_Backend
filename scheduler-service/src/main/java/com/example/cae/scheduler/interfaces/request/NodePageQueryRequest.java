package com.example.cae.scheduler.interfaces.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class NodePageQueryRequest {
	@Min(value = 1, message = "pageNum必须大于等于1")
	private Integer pageNum;
	@Min(value = 1, message = "pageSize必须大于等于1")
	@Max(value = 200, message = "pageSize不能超过200")
	private Integer pageSize;
	private String nodeName;
	private String status;
	private Integer enabled;
	private String solverId;

	public Integer getPageNum() {
		return pageNum;
	}

	public void setPageNum(Integer pageNum) {
		this.pageNum = pageNum;
	}

	public Integer getPageSize() {
		return pageSize;
	}

	public void setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
	}

	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Integer getEnabled() {
		return enabled;
	}

	public void setEnabled(Integer enabled) {
		this.enabled = enabled;
	}

	public String getSolverId() {
		return solverId;
	}

	public void setSolverId(String solverId) {
		this.solverId = solverId;
	}

	public Long getSolverIdAsLong() {
		if (solverId == null) {
			return null;
		}
		String value = solverId.trim();
		if (value.isEmpty() || "undefined".equalsIgnoreCase(value) || "null".equalsIgnoreCase(value)) {
			return null;
		}
		try {
			return Long.valueOf(value);
		} catch (NumberFormatException ex) {
			return null;
		}
	}
}
