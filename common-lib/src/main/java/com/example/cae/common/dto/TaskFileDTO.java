package com.example.cae.common.dto;

public class TaskFileDTO {
    private Long taskId;
    private String fileKey;
    private String originName;
    private String storagePath;
    private String unpackDir;
    private String relativePath;
    private Integer archiveFlag;
    private Long fileSize;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getFileKey() {
        return fileKey;
    }

    public void setFileKey(String fileKey) {
        this.fileKey = fileKey;
    }

    public String getOriginName() {
        return originName;
    }

    public void setOriginName(String originName) {
        this.originName = originName;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public String getUnpackDir() {
        return unpackDir;
    }

    public void setUnpackDir(String unpackDir) {
        this.unpackDir = unpackDir;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public Integer getArchiveFlag() {
        return archiveFlag;
    }

    public void setArchiveFlag(Integer archiveFlag) {
        this.archiveFlag = archiveFlag;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
}
