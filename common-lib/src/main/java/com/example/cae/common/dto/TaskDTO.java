package com.example.cae.common.dto;

public class TaskDTO {
    private Long taskId;
    private String taskNo;
    private String taskName;
    private Long solverId;
    private String solverCode;
    private String solverExecMode;
    private String solverExecPath;
    private Long profileId;
    private String taskType;
    private String commandTemplate;
    private String parserName;
    private Integer timeoutSeconds;
    private Integer priority;
    private String paramsJson;
    private java.util.List<TaskFileDTO> inputFiles;
    private java.util.Map<String, Object> params;
    private Long nodeId;
    private java.time.LocalDateTime submitTime;

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

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public Long getSolverId() {
        return solverId;
    }

    public void setSolverId(Long solverId) {
        this.solverId = solverId;
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

    public String getParamsJson() {
        return paramsJson;
    }

    public void setParamsJson(String paramsJson) {
        this.paramsJson = paramsJson;
    }

    public Long getNodeId() {
        return nodeId;
    }

    public void setNodeId(Long nodeId) {
        this.nodeId = nodeId;
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

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public java.util.List<TaskFileDTO> getInputFiles() {
        return inputFiles;
    }

    public void setInputFiles(java.util.List<TaskFileDTO> inputFiles) {
        this.inputFiles = inputFiles;
    }

    public java.util.Map<String, Object> getParams() {
        return params;
    }

    public void setParams(java.util.Map<String, Object> params) {
        this.params = params;
    }

    public java.time.LocalDateTime getSubmitTime() {
        return submitTime;
    }

    public void setSubmitTime(java.time.LocalDateTime submitTime) {
        this.submitTime = submitTime;
    }
}
