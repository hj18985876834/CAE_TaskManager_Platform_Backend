package com.example.cae.solver.interfaces.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class SolverPageQueryRequest {
	@Min(value = 1, message = "pageNum必须大于等于1")
	private Integer pageNum;
	@Min(value = 1, message = "pageSize必须大于等于1")
	@Max(value = 200, message = "pageSize不能超过200")
	private Integer pageSize;
	@Size(max = 50, message = "solverCode长度不能超过50")
	private String solverCode;
	@Size(max = 100, message = "solverName长度不能超过100")
	private String solverName;
	@Min(value = 0, message = "enabled只能为0或1")
	@Max(value = 1, message = "enabled只能为0或1")
	private Integer enabled;

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

	public String getSolverCode() {
		return solverCode;
	}

	public void setSolverCode(String solverCode) {
		this.solverCode = solverCode;
	}

	public String getSolverName() {
		return solverName;
	}

	public void setSolverName(String solverName) {
		this.solverName = solverName;
	}

	public Integer getEnabled() {
		return enabled;
	}

	public void setEnabled(Integer enabled) {
		this.enabled = enabled;
	}
}

