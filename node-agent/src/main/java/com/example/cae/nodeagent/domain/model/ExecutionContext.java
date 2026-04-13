package com.example.cae.nodeagent.domain.model;

import java.util.List;
import java.util.Map;

public class ExecutionContext {
	private Long taskId;
	private String taskNo;
	private Long solverId;
	private String solverCode;
	private Long profileId;
	private String taskType;
	private String commandTemplate;
	private String parserName;
	private Integer timeoutSeconds;
	private List<InputFileMeta> inputFiles;
	private Map<String, Object> params;
	private String workDir;
	private String taskDir;
	private String inputDir;
	private String outputDir;
	private String logDir;

	public boolean isTimeoutEnabled() {
		return timeoutSeconds != null && timeoutSeconds > 0;
	}

	public boolean hasInputFiles() {
		return inputFiles != null && !inputFiles.isEmpty();
	}

	public boolean hasParser() {
		return parserName != null && !parserName.trim().isEmpty();
	}

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

	public String getWorkDir() {
		return workDir;
	}

	public void setWorkDir(String workDir) {
		this.workDir = workDir;
	}

	public String getInputDir() {
		return inputDir;
	}

	public String getTaskDir() {
		return taskDir;
	}

	public void setTaskDir(String taskDir) {
		this.taskDir = taskDir;
	}

	public void setInputDir(String inputDir) {
		this.inputDir = inputDir;
	}

	public String getOutputDir() {
		return outputDir;
	}

	public void setOutputDir(String outputDir) {
		this.outputDir = outputDir;
	}

	public String getLogDir() {
		return logDir;
	}

	public void setLogDir(String logDir) {
		this.logDir = logDir;
	}
}
