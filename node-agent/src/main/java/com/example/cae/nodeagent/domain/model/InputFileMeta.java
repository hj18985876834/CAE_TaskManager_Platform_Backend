package com.example.cae.nodeagent.domain.model;

public class InputFileMeta {
	private String fileKey;
	private String originName;
	private String storagePath;
	private String unpackDir;

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
}
