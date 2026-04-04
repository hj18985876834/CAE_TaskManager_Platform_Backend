package com.example.cae.nodeagent.infrastructure.storage;

import com.example.cae.nodeagent.domain.model.ExecutionContext;
import com.example.cae.nodeagent.domain.model.InputFileMeta;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class InputFilePrepareService {
	private final PathMappingSupport pathMappingSupport;

	public InputFilePrepareService(PathMappingSupport pathMappingSupport) {
		this.pathMappingSupport = pathMappingSupport;
	}

	public void prepare(ExecutionContext context) {
		if (!context.hasInputFiles()) {
			return;
		}
		for (InputFileMeta inputFile : context.getInputFiles()) {
			if (inputFile.getStoragePath() == null || inputFile.getStoragePath().trim().isEmpty()) {
				continue;
			}
			String mappedPath = pathMappingSupport.toLinuxPath(inputFile.getStoragePath());
			File source = new File(mappedPath);
			if (!source.exists() || source.isDirectory()) {
				continue;
			}
			String targetName = inputFile.getOriginName() == null || inputFile.getOriginName().trim().isEmpty()
					? source.getName()
					: inputFile.getOriginName();
			Path target = Path.of(context.getInputDir(), targetName);
			try {
				Files.copy(source.toPath(), target);
			} catch (IOException ex) {
				throw new RuntimeException("copy input file failed: " + source.getAbsolutePath(), ex);
			}
		}
	}
}

