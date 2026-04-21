package com.example.cae.nodeagent.infrastructure.storage;

import com.example.cae.nodeagent.domain.model.ExecutionContext;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;

@Component
public class ResultFileCollector {
	public List<File> collect(ExecutionContext context) {
		if (context == null || context.getOutputDir() == null || context.getOutputDir().isBlank()) {
			return Collections.emptyList();
		}
		Path outputDir = Path.of(context.getOutputDir()).normalize();
		if (!Files.exists(outputDir) || !Files.isDirectory(outputDir)) {
			return Collections.emptyList();
		}
		try (var stream = Files.walk(outputDir)) {
			return stream
					.filter(Files::isRegularFile)
					.sorted(Comparator.comparing(path -> outputDir.relativize(path).toString()))
					.map(Path::toFile)
					.toList();
		} catch (Exception ex) {
			throw new IllegalStateException("collect result files failed, outputDir=" + outputDir, ex);
		}
	}
}
