package com.example.cae.scheduler.interfaces.request;

import com.example.cae.common.constant.QueryValidationConstants;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class NodePageQueryRequest {
	@Min(value = 1, message = "pageNum必须大于等于1")
	private Integer pageNum;
	@Min(value = 1, message = "pageSize必须大于等于1")
	@Max(value = 200, message = "pageSize不能超过200")
	private Integer pageSize;
	@Size(max = QueryValidationConstants.NODE_NAME_MAX_LENGTH, message = "nodeName长度不能超过100")
	private String nodeName;
	@Size(max = QueryValidationConstants.STATUS_MAX_LENGTH, message = "status长度不能超过30")
	private String status;
	@Min(value = 0, message = "enabled只能是0或1")
	@Max(value = 1, message = "enabled只能是0或1")
	private Integer enabled;
	@Positive(message = "solverId必须大于0")
	private Long solverId;

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

	public Long getSolverId() {
		return solverId;
	}

	public void setSolverId(Long solverId) {
		this.solverId = solverId;
	}
}
