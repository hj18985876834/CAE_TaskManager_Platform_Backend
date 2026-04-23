package com.example.cae.task.interfaces.response;

import java.time.LocalDateTime;

public class TaskInputFileResponse {
	private Long fileId;
	private String fileRole;
	private String fileKey;
	private String originName;
	private String storagePath;
	private String unpackDir;
	private Long fileSize;
	private String fileSuffix;
	private LocalDateTime createdAt;

	public Long getFileId() {
		return fileId;
	}

	public void setFileId(Long fileId) {
		this.fileId = fileId;
	}

	public String getFileRole() {
		return fileRole;
	}

	public void setFileRole(String fileRole) {
		this.fileRole = fileRole;
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

	public Long getFileSize() {
		return fileSize;
	}

	public void setFileSize(Long fileSize) {
		this.fileSize = fileSize;
	}

	public String getFileSuffix() {
		return fileSuffix;
	}

	public void setFileSuffix(String fileSuffix) {
		this.fileSuffix = fileSuffix;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
}
