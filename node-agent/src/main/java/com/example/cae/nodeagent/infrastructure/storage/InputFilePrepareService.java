package com.example.cae.nodeagent.infrastructure.storage;

import com.example.cae.nodeagent.domain.model.ExecutionContext;
import com.example.cae.nodeagent.domain.model.InputFileMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class InputFilePrepareService {
	private static final Logger log = LoggerFactory.getLogger(InputFilePrepareService.class);

	private final PathMappingSupport pathMappingSupport;

	public InputFilePrepareService(PathMappingSupport pathMappingSupport) {
		this.pathMappingSupport = pathMappingSupport;
	}

	public void prepare(ExecutionContext context) {
		context.setTaskDir(context.getWorkDir());
		if (!context.hasInputFiles()) {
			return;
		}
		String validatedTaskDir = null;
		for (InputFileMeta inputFile : context.getInputFiles()) {
			if (inputFile.getUnpackDir() != null && !inputFile.getUnpackDir().isBlank()) {
				String candidateTaskDir = resolveValidatedTaskDir(inputFile, context.getTaskId());
				if (validatedTaskDir == null) {
					validatedTaskDir = candidateTaskDir;
					context.setTaskDir(candidateTaskDir);
					log.info("reusing validated unpackDir for taskId={}, taskDir={}", context.getTaskId(), candidateTaskDir);
				} else if (!Objects.equals(validatedTaskDir, candidateTaskDir)) {
					throw new IllegalStateException("multiple unpackDir values found for taskId=" + context.getTaskId());
				}
				continue;
			}
			if (inputFile.getStoragePath() == null || inputFile.getStoragePath().trim().isEmpty()) {
				throw new IllegalStateException("input file storagePath is empty, taskId=" + context.getTaskId());
			}
			String mappedPath = pathMappingSupport.toLinuxPath(inputFile.getStoragePath());
			File source = new File(mappedPath);
			if (!source.exists()) {
				throw new IllegalStateException("input file not found on node-agent, taskId="
						+ context.getTaskId()
						+ ", originalPath=" + inputFile.getStoragePath()
						+ ", mappedPath=" + mappedPath);
			}
			if (source.isDirectory()) {
				throw new IllegalStateException("input file path points to a directory, taskId="
						+ context.getTaskId()
						+ ", originalPath=" + inputFile.getStoragePath()
						+ ", mappedPath=" + mappedPath);
			}
			String targetName = inputFile.getOriginName() == null || inputFile.getOriginName().trim().isEmpty()
					? source.getName()
					: inputFile.getOriginName();
			Path target = Path.of(context.getInputDir(), targetName);
			try {
				if (!isSamePath(source.toPath(), target)) {
					Files.copy(source.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
					log.info("copied input file for taskId={}, source={}, target={}", context.getTaskId(), source.getAbsolutePath(), target);
				}
				if (isZipFile(targetName)) {
					Path taskDir = extractArchive(target, Path.of(context.getWorkDir()));
					context.setTaskDir(normalize(taskDir));
					log.info("extracted archive for taskId={}, archive={}, taskDir={}", context.getTaskId(), target, context.getTaskDir());
				}
			} catch (IOException ex) {
				throw new RuntimeException("copy input file failed: " + source.getAbsolutePath(), ex);
			}
		}
	}

	private boolean isZipFile(String fileName) {
		return fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".zip");
	}

	private String resolveValidatedTaskDir(InputFileMeta inputFile, Long taskId) {
		Path unpackDir = Path.of(pathMappingSupport.toLinuxPath(inputFile.getUnpackDir())).normalize();
		if (!Files.exists(unpackDir)) {
			throw new IllegalStateException("validated unpackDir not found on node-agent, taskId="
					+ taskId
					+ ", originalPath=" + inputFile.getUnpackDir()
					+ ", mappedPath=" + unpackDir);
		}
		if (!Files.isDirectory(unpackDir)) {
			throw new IllegalStateException("validated unpackDir is not a directory, taskId="
					+ taskId
					+ ", originalPath=" + inputFile.getUnpackDir()
					+ ", mappedPath=" + unpackDir);
		}
		return normalize(unpackDir);
	}

	private Path extractArchive(Path archivePath, Path workDir) throws IOException {
		Set<String> topLevelDirs = new HashSet<>();
		boolean hasTopLevelFile = false;
		try (InputStream inputStream = Files.newInputStream(archivePath);
			 ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
			ZipEntry entry;
			while ((entry = zipInputStream.getNextEntry()) != null) {
				String entryName = normalizeEntry(entry.getName());
				if (entryName.isBlank()) {
					continue;
				}
				String topLevel = firstSegment(entryName);
				if (topLevel != null && !topLevel.isBlank()) {
					if (entryName.contains("/")) {
						topLevelDirs.add(topLevel);
					} else if (!entry.isDirectory()) {
						hasTopLevelFile = true;
					}
				}
				Path target = workDir.resolve(entryName).normalize();
				if (!target.startsWith(workDir)) {
					throw new IOException("unsafe zip entry: " + entry.getName());
				}
					if (entry.isDirectory()) {
						Files.createDirectories(target);
					} else {
						if (target.getParent() != null) {
							Files.createDirectories(target.getParent());
						}
						Files.copy(zipInputStream, target, StandardCopyOption.REPLACE_EXISTING);
					}
				}
			}
		if (!hasTopLevelFile && topLevelDirs.size() == 1) {
			return workDir.resolve(topLevelDirs.iterator().next()).normalize();
		}
		return workDir;
	}

	private String normalize(String path) {
		return path == null ? null : path.replace("\\", "/");
	}

	private String normalize(Path path) {
		return path == null ? null : normalize(path.toString());
	}

	private String normalizeEntry(String path) {
		if (path == null) {
			return "";
		}
		String normalized = path.replace("\\", "/").trim();
		while (normalized.startsWith("/")) {
			normalized = normalized.substring(1);
		}
		if (normalized.startsWith("./")) {
			normalized = normalized.substring(2);
		}
		return normalized;
	}

	private String firstSegment(String path) {
		if (path == null || path.isBlank()) {
			return null;
		}
		int idx = path.indexOf('/');
		return idx < 0 ? path : path.substring(0, idx);
	}

	private boolean isSamePath(Path source, Path target) throws IOException {
		Path normalizedSource = source.toAbsolutePath().normalize();
		Path normalizedTarget = target.toAbsolutePath().normalize();
		if (Files.exists(normalizedTarget) && Files.isSameFile(normalizedSource, normalizedTarget)) {
			return true;
		}
		return normalizedSource.equals(normalizedTarget);
	}
}
