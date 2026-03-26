package com.example.cae.nodeagent.infrastructure.storage;

import com.example.cae.nodeagent.domain.model.ExecutionContext;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class ResultFileCollector {
	public List<File> collect(ExecutionContext context) {
		File outputDir = new File(context.getOutputDir());
		File[] files = outputDir.listFiles();
		if (files == null) {
			return Collections.emptyList();
		}
		return Arrays.stream(files).filter(File::isFile).toList();
	}
}

