package com.example.cae.common.dto;

public class SolverProfileDTO {
    private Long profileId;
    private Long solverId;
    private String profileCode;
    private String taskType;

    public Long getProfileId() {
        return profileId;
    }

    public void setProfileId(Long profileId) {
        this.profileId = profileId;
    }

    public Long getSolverId() {
        return solverId;
    }

    public void setSolverId(Long solverId) {
        this.solverId = solverId;
    }

    public String getProfileCode() {
        return profileCode;
    }

    public void setProfileCode(String profileCode) {
        this.profileCode = profileCode;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }
}
