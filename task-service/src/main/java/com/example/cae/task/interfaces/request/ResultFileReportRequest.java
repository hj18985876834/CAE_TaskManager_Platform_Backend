package com.example.cae.task.interfaces.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class ResultFileReportRequest {
	@NotNull(message = "nodeId不能为空")
	@Positive(message = "nodeId必须大于0")
	private Long nodeId;
	@NotBlank(message = "fileType不能为空")
	@Size(max = 32, message = "fileType长度不能超过32")
	private String fileType;
	@NotBlank(message = "fileName不能为空")
	@Size(max = 255, message = "fileName长度不能超过255")
	private String fileName;
	@NotBlank(message = "storagePath不能为空")
	@Size(max = 500, message = "storagePath长度不能超过500")
	private String storagePath;
	@NotNull(message = "fileSize不能为空")
	@Min(value = 0, message = "fileSize不能小于0")
	private Long fileSize;

	public Long getNodeId() {
		return nodeId;
	}

	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

	public String getFileType() {
		return fileType;
	}

	public void setFileType(String fileType) {
		this.fileType = fileType;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getStoragePath() {
		return storagePath;
	}

	public void setStoragePath(String storagePath) {
		this.storagePath = storagePath;
	}

	public Long getFileSize() {
		return fileSize;
	}

	public void setFileSize(Long fileSize) {
		this.fileSize = fileSize;
	}
}
