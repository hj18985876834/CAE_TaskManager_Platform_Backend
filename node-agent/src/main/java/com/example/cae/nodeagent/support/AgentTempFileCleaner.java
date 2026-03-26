package com.example.cae.nodeagent.support;

import com.example.cae.nodeagent.config.NodeAgentConfig;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.Instant;

@Component
public class AgentTempFileCleaner {
	private final NodeAgentConfig nodeAgentConfig;

	public AgentTempFileCleaner(NodeAgentConfig nodeAgentConfig) {
		this.nodeAgentConfig = nodeAgentConfig;
	}

	@Scheduled(fixedDelayString = "${cae.node.temp-clean-interval-ms:3600000}")
	public void cleanOldTaskDirs() {
		File rootDir = new File(nodeAgentConfig.getWorkRoot());
		if (!rootDir.exists() || !rootDir.isDirectory()) {
			return;
		}
		long now = Instant.now().toEpochMilli();
		long ttl = 24L * 60 * 60 * 1000;
		File[] dirs = rootDir.listFiles(File::isDirectory);
		if (dirs == null) {
			return;
		}
		for (File dir : dirs) {
			if (now - dir.lastModified() > ttl) {
				deleteRecursively(dir);
			}
		}
	}

	private void deleteRecursively(File file) {
		if (file == null || !file.exists()) {
			return;
		}
		if (file.isDirectory()) {
			File[] children = file.listFiles();
			if (children != null) {
				for (File child : children) {
					deleteRecursively(child);
				}
			}
		}
		file.delete();
	}
}