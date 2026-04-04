package com.example.cae.task.infrastructure.persistence.entity;

import java.time.LocalDateTime;

public class TaskFilePO {
	private Long id;
	private Long taskId;
	private String fileRole;
	private String fileKey;
	private String originName;
	private String storagePath;
	private String unpackDir;
	private String relativePath;
	private Integer archiveFlag;
	private Long fileSize;
	private String fileSuffix;
	private String checksum;
	private LocalDateTime createdAt;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public Long getTaskId() { return taskId; }
	public void setTaskId(Long taskId) { this.taskId = taskId; }
	public String getFileRole() { return fileRole; }
	public void setFileRole(String fileRole) { this.fileRole = fileRole; }
	public String getFileKey() { return fileKey; }
	public void setFileKey(String fileKey) { this.fileKey = fileKey; }
	public String getOriginName() { return originName; }
	public void setOriginName(String originName) { this.originName = originName; }
	public String getStoragePath() { return storagePath; }
	public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
	public String getUnpackDir() { return unpackDir; }
	public void setUnpackDir(String unpackDir) { this.unpackDir = unpackDir; }
	public String getRelativePath() { return relativePath; }
	public void setRelativePath(String relativePath) { this.relativePath = relativePath; }
	public Integer getArchiveFlag() { return archiveFlag; }
	public void setArchiveFlag(Integer archiveFlag) { this.archiveFlag = archiveFlag; }
	public Long getFileSize() { return fileSize; }
	public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
	public String getFileSuffix() { return fileSuffix; }
	public void setFileSuffix(String fileSuffix) { this.fileSuffix = fileSuffix; }
	public String getChecksum() { return checksum; }
	public void setChecksum(String checksum) { this.checksum = checksum; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

