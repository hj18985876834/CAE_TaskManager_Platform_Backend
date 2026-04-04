package com.example.cae.nodeagent.infrastructure.storage;

import com.example.cae.nodeagent.config.NodeAgentConfig;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class PathMappingSupport {
	private final NodeAgentConfig nodeAgentConfig;

	public PathMappingSupport(NodeAgentConfig nodeAgentConfig) {
		this.nodeAgentConfig = nodeAgentConfig;
	}

	public String toLinuxPath(String windowsPath) {
		if (windowsPath == null) return null;
		
		String winPrefix = nodeAgentConfig.getPathMappingWindows();
		String linPrefix = nodeAgentConfig.getPathMappingLinux();
		
		if (winPrefix != null && linPrefix != null && !winPrefix.isBlank() && !linPrefix.isBlank()) {
			String normalizedWin = normalizeWin(windowsPath);
			String normalizedPref = normalizeWin(winPrefix);
			
			if (normalizedWin.startsWith(normalizedPref)) {
				String suffix = normalizedWin.substring(normalizedPref.length());
				String mapped = linPrefix + suffix;
				return mapped.replace("\\", "/").replaceAll("//+", "/");
			}
		}
		return windowsPath.replace("\\", "/");
	}

	public String toWindowsPath(String linuxPath) {
		if (linuxPath == null) return null;
		
		String winPrefix = nodeAgentConfig.getPathMappingWindows();
		String linPrefix = nodeAgentConfig.getPathMappingLinux();
		
		if (winPrefix != null && linPrefix != null && !winPrefix.isBlank() && !linPrefix.isBlank()) {
			String normalizedLin = linuxPath.replace("\\", "/");
			String normalizedPref = linPrefix.replace("\\", "/");
			
			if (normalizedLin.startsWith(normalizedPref)) {
				String suffix = normalizedLin.substring(normalizedPref.length());
				String mapped = winPrefix + suffix;
				return mapped.replace("/", "\\").replaceAll("\\\\\\\\+", "\\\\");
			}
		}
		return linuxPath;
	}

	private String normalizeWin(String path) {
		return path.replace("/", "\\").replaceAll("\\\\\\\\+", "\\\\");
	}
}
