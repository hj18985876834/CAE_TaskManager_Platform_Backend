package com.example.cae.solver.interfaces.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateSolverRequest {
	@NotBlank(message = "solverName不能为空")
	@Size(max = 100, message = "solverName长度不能超过100")
	private String solverName;
	@NotBlank(message = "version不能为空")
	@Size(max = 50, message = "version长度不能超过50")
	private String version;
	@NotBlank(message = "execMode不能为空")
	private String execMode;
	@NotBlank(message = "execPath不能为空")
	@Size(max = 255, message = "execPath长度不能超过255")
	private String execPath;
	@Size(max = 255, message = "description长度不能超过255")
	private String description;

	public String getSolverName() {
		return solverName;
	}

	public void setSolverName(String solverName) {
		this.solverName = solverName;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getExecMode() {
		return execMode;
	}

	public void setExecMode(String execMode) {
		this.execMode = execMode;
	}

	public String getExecPath() {
		return execPath;
	}

	public void setExecPath(String execPath) {
		this.execPath = execPath;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
