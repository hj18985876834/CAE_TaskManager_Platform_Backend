package com.example.cae.nodeagent.interfaces.request;

import com.example.cae.nodeagent.domain.model.InputFileMeta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public class DispatchTaskRequest {
	@NotNull(message = "taskId不能为空")
	@Positive(message = "taskId必须大于0")
	private Long taskId;
	@NotBlank(message = "taskNo不能为空")
	@Size(max = 50, message = "taskNo长度不能超过50")
	private String taskNo;
	@NotNull(message = "solverId不能为空")
	@Positive(message = "solverId必须大于0")
	private Long solverId;
	@Size(max = 50, message = "solverCode长度不能超过50")
	private String solverCode;
	private String solverExecMode;
	private String solverExecPath;
	private Long profileId;
	@NotBlank(message = "taskType不能为空")
	@Size(max = 50, message = "taskType长度不能超过50")
	private String taskType;
	@NotBlank(message = "commandTemplate不能为空")
	@Size(max = 255, message = "commandTemplate长度不能超过255")
	private String commandTemplate;
	@Size(max = 100, message = "parserName长度不能超过100")
	private String parserName;
	@Positive(message = "timeoutSeconds必须大于0")
	private Integer timeoutSeconds;
	@Valid
	private List<InputFileMeta> inputFiles;
	private Map<String, Object> params;

	public Long getTaskId() {
		return taskId;
	}

	public void setTaskId(Long taskId) {
		this.taskId = taskId;
	}

	public String getTaskNo() {
		return taskNo;
	}

	public void setTaskNo(String taskNo) {
		this.taskNo = taskNo;
	}

	public Long getSolverId() {
		return solverId;
	}

	public void setSolverId(Long solverId) {
		this.solverId = solverId;
	}

	public String getSolverCode() {
		return solverCode;
	}

	public void setSolverCode(String solverCode) {
		this.solverCode = solverCode;
	}

	public String getSolverExecMode() {
		return solverExecMode;
	}

	public void setSolverExecMode(String solverExecMode) {
		this.solverExecMode = solverExecMode;
	}

	public String getSolverExecPath() {
		return solverExecPath;
	}

	public void setSolverExecPath(String solverExecPath) {
		this.solverExecPath = solverExecPath;
	}

	public Long getProfileId() {
		return profileId;
	}

	public void setProfileId(Long profileId) {
		this.profileId = profileId;
	}

	public String getTaskType() {
		return taskType;
	}

	public void setTaskType(String taskType) {
		this.taskType = taskType;
	}

	public String getCommandTemplate() {
		return commandTemplate;
	}

	public void setCommandTemplate(String commandTemplate) {
		this.commandTemplate = commandTemplate;
	}

	public String getParserName() {
		return parserName;
	}

	public void setParserName(String parserName) {
		this.parserName = parserName;
	}

	public Integer getTimeoutSeconds() {
		return timeoutSeconds;
	}

	public void setTimeoutSeconds(Integer timeoutSeconds) {
		this.timeoutSeconds = timeoutSeconds;
	}

	public List<InputFileMeta> getInputFiles() {
		return inputFiles;
	}

	public void setInputFiles(List<InputFileMeta> inputFiles) {
		this.inputFiles = inputFiles;
	}

	public Map<String, Object> getParams() {
		return params;
	}

	public void setParams(Map<String, Object> params) {
		this.params = params;
	}
}
