package com.example.cae.task.application.manager;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.dto.FileRuleDTO;
import com.example.cae.common.enums.OperatorTypeEnum;
import com.example.cae.common.enums.TaskStatusEnum;
import com.example.cae.common.exception.BizException;
import com.example.cae.task.domain.model.Task;
import com.example.cae.task.domain.model.TaskFile;
import com.example.cae.task.domain.repository.TaskFileRepository;
import com.example.cae.task.domain.repository.TaskRepository;
import com.example.cae.task.domain.service.TaskStatusDomainService;
import com.example.cae.task.domain.service.TaskValidationDomainService;
import com.example.cae.task.infrastructure.client.SolverClient;
import com.example.cae.task.infrastructure.support.TaskPathResolver;
import com.example.cae.task.infrastructure.support.TaskStoragePathSupport;
import com.example.cae.task.interfaces.response.TaskValidateResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class TaskValidationManager {
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

	private final TaskRepository taskRepository;
	private final TaskFileRepository taskFileRepository;
	private final SolverClient solverClient;
	private final TaskStatusDomainService taskStatusDomainService;
	private final TaskValidationDomainService taskValidationDomainService;
 	private final TaskPathResolver taskPathResolver;
	private final TaskStoragePathSupport taskStoragePathSupport;

	public TaskValidationManager(TaskRepository taskRepository,
							 TaskFileRepository taskFileRepository,
							 SolverClient solverClient,
							 TaskStatusDomainService taskStatusDomainService,
							 TaskValidationDomainService taskValidationDomainService,
							 TaskPathResolver taskPathResolver,
							 TaskStoragePathSupport taskStoragePathSupport) {
		this.taskRepository = taskRepository;
		this.taskFileRepository = taskFileRepository;
		this.solverClient = solverClient;
		this.taskStatusDomainService = taskStatusDomainService;
		this.taskValidationDomainService = taskValidationDomainService;
		this.taskPathResolver = taskPathResolver;
		this.taskStoragePathSupport = taskStoragePathSupport;
	}

	public TaskValidateResponse validateTask(Long taskId, Long userId) {
		Task task = loadAndCheckOwner(taskId, userId);
		List<TaskFile> files = taskFileRepository.listByTaskId(taskId);
		Long profileSolverId = solverClient.getProfileSolverId(task.getProfileId());
		String profileTaskType = solverClient.getProfileTaskType(task.getProfileId());
		List<FileRuleDTO> rules = solverClient.getFileRules(task.getProfileId());
		List<TaskValidateResponse.ValidationIssue> issues = new ArrayList<>();

		try {
			taskValidationDomainService.checkTaskEditable(task);
			if (profileSolverId == null) {
				throw new BizException(ErrorCodeConstants.PROFILE_NOT_FOUND, "profile not found");
			}
			if (!profileSolverId.equals(task.getSolverId())) {
				throw new BizException(ErrorCodeConstants.TASK_PROFILE_MISMATCH, "solver and profile do not match");
			}
			if (profileTaskType != null && !profileTaskType.isBlank() && !profileTaskType.equals(task.getTaskType())) {
				throw new BizException(ErrorCodeConstants.TASK_TYPE_MISMATCH, "task type and profile do not match");
			}
			validateArchiveAndRules(task, files, rules, issues);
			if (!issues.isEmpty()) {
				throw new BizException(ErrorCodeConstants.TASK_VALIDATION_FAILED, "任务文件校验失败", buildInvalidResponse(task, issues));
			}
		} catch (BizException ex) {
			if (shouldAttachValidationData(ex)) {
				throw new BizException(ex.getCode(), ex.getMessage(), buildInvalidResponse(task, issues));
			}
			throw ex;
		}

		if (!TaskStatusEnum.VALIDATED.name().equals(task.getStatus())) {
			taskStatusDomainService.transfer(task, TaskStatusEnum.VALIDATED.name(), "validation passed", OperatorTypeEnum.USER.name(), userId);
			taskRepository.update(task);
		}
		TaskValidateResponse response = new TaskValidateResponse();
		response.setTaskId(taskId);
		response.setValid(Boolean.TRUE);
		response.setStatus(task.getStatus());
		response.setIssues(List.of());
		return response;
	}

	private void validateArchiveAndRules(Task task, List<TaskFile> files, List<FileRuleDTO> rules, List<TaskValidateResponse.ValidationIssue> issues) {
		TaskFile archive = files.stream()
				.filter(file -> "ARCHIVE".equalsIgnoreCase(file.getFileRole()) || "input_archive".equalsIgnoreCase(file.getFileKey()))
				.findFirst()
				.orElse(null);
		if (archive == null) {
			issues.add(issue("input_archive", null, "ARCHIVE_MISSING", "缺少输入压缩包 input_archive"));
			return;
		}
		String suffix = archive.getFileSuffix() == null ? "" : archive.getFileSuffix().toLowerCase(Locale.ROOT);
		if (!"zip".equals(suffix)) {
			issues.add(issue("input_archive", archive.getOriginName(), "INVALID_ARCHIVE_SUFFIX", "仅支持 zip 压缩包"));
			return;
		}
		ExtractedContext extracted = extractArchive(task, archive, issues);
		if (extracted == null) {
			return;
		}
		for (FileRuleDTO rule : rules) {
			if (shouldSkipExtractedRuleValidation(rule)) {
				continue;
			}
			validateRule(rule, extracted.entries(), issues);
		}
	}

	private boolean shouldSkipExtractedRuleValidation(FileRuleDTO rule) {
		if (rule == null) {
			return true;
		}
		if ("input_archive".equalsIgnoreCase(rule.getFileKey())) {
			return true;
		}
		if (!isBlank(rule.getFileType()) && "ZIP".equalsIgnoreCase(rule.getFileType())) {
			return true;
		}
		return false;
	}

	private ExtractedContext extractArchive(Task task, TaskFile archive, List<TaskValidateResponse.ValidationIssue> issues) {
		Path archivePath = Path.of(taskStoragePathSupport.toAbsoluteTaskPath(archive.getStoragePath())).normalize();
		Path workDir = Path.of(taskPathResolver.resolveTaskRoot(task.getId()), "workdir").normalize();
		try {
			cleanupDirectory(workDir);
			Files.createDirectories(workDir);
			try (InputStream inputStream = Files.newInputStream(archivePath);
				 ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
				ZipEntry entry;
				while ((entry = zipInputStream.getNextEntry()) != null) {
					String entryName = normalizeRelativePath(entry.getName());
					if (entryName.isBlank()) {
						continue;
					}
					Path target = workDir.resolve(entryName).normalize();
					if (!target.startsWith(workDir)) {
						issues.add(issue("input_archive", entry.getName(), "ARCHIVE_UNSAFE_PATH", "压缩包路径非法，存在目录穿越风险"));
						return null;
					}
					if (entry.isDirectory()) {
						Files.createDirectories(target);
					} else {
						Files.createDirectories(target.getParent());
						Files.copy(zipInputStream, target);
					}
				}
			}
			archive.setUnpackDir(taskStoragePathSupport.toStoredTaskPath(workDir.toString()));
			archive.setArchiveFlag(1);
			taskFileRepository.update(archive);
			return new ExtractedContext(workDir, scanEntries(workDir));
		} catch (IOException ex) {
			issues.add(issue("input_archive", archive.getOriginName(), "ARCHIVE_BROKEN", "压缩包损坏或不可解压"));
			return null;
		}
	}

	private List<ExtractedEntry> scanEntries(Path root) throws IOException {
		if (!Files.exists(root)) {
			return List.of();
		}
		try (var stream = Files.walk(root)) {
			return stream
					.filter(path -> !path.equals(root))
					.map(path -> {
						String relativePath = normalizeRelativePath(root.relativize(path).toString());
						boolean isDirectory = Files.isDirectory(path);
						String name = path.getFileName() == null ? relativePath : path.getFileName().toString();
						return new ExtractedEntry(relativePath, isDirectory, name, extractSuffix(name));
					})
					.toList();
		}
	}

	private void validateRule(FileRuleDTO rule, List<ExtractedEntry> entries, List<TaskValidateResponse.ValidationIssue> issues) {
		String pathPattern = firstNonBlank(rule.getPathPattern(), rule.getFileNamePattern());
		if (pathPattern == null) {
			return;
		}
		List<ExtractedEntry> patternMatched = entries.stream()
				.filter(entry -> pathMatches(pathPattern, entry.relativePath()))
				.toList();
		List<ExtractedEntry> typedMatched = applyTypeFilter(patternMatched, rule.getFileType());
		List<ExtractedEntry> namedMatched = applyNameFilter(typedMatched, rule.getFileNamePattern());

		if (rule.getRequiredFlag() != null && rule.getRequiredFlag() == 1) {
			if (patternMatched.isEmpty()) {
				issues.add(issue(rule.getFileKey(), pathPattern, "MISSING_REQUIRED_PATH", "缺少必需路径 " + pathPattern));
				return;
			}
			if (!typedMatched.isEmpty() || !isBlank(rule.getFileType())) {
				if (typedMatched.isEmpty()) {
					issues.add(issue(rule.getFileKey(), pathPattern, "INVALID_FILE_TYPE", "路径类型与模板规则不匹配"));
					return;
				}
			}
			if (!isBlank(rule.getFileNamePattern()) && namedMatched.isEmpty()) {
				issues.add(issue(rule.getFileKey(), pathPattern, "INVALID_FILENAME_PATTERN", "文件名不匹配规则 " + rule.getFileNamePattern()));
				return;
			}
		}

		Map<String, Object> ruleJson = parseRuleJson(rule, issues);
		if (ruleJson == null) {
			return;
		}
		List<ExtractedEntry> effective = namedMatched;
		if (!checkCountConstraint(rule, effective, ruleJson, issues)) {
			return;
		}
		checkSuffixConstraint(rule, effective, ruleJson, issues);
	}

	private List<ExtractedEntry> applyTypeFilter(List<ExtractedEntry> entries, String fileType) {
		if (isBlank(fileType)) {
			return entries;
		}
		String normalized = fileType.trim().toUpperCase(Locale.ROOT);
		return entries.stream()
				.filter(entry -> switch (normalized) {
					case "DIR", "DIRECTORY" -> entry.directory();
					case "FILE" -> !entry.directory();
					default -> true;
				})
				.toList();
	}

	private List<ExtractedEntry> applyNameFilter(List<ExtractedEntry> entries, String fileNamePattern) {
		if (isBlank(fileNamePattern)) {
			return entries;
		}
		String pattern = fileNamePattern.trim();
		return entries.stream()
				.filter(entry -> globMatch(pattern, entry.name()))
				.toList();
	}

	private Map<String, Object> parseRuleJson(FileRuleDTO rule, List<TaskValidateResponse.ValidationIssue> issues) {
		if (isBlank(rule.getRuleJson())) {
			return Map.of();
		}
		try {
			return OBJECT_MAPPER.readValue(rule.getRuleJson(), MAP_TYPE);
		} catch (Exception ex) {
			issues.add(issue(rule.getFileKey(), rule.getPathPattern(), "TEMPLATE_RULE_CONFLICT", "模板规则 JSON 非法"));
			return null;
		}
	}

	private boolean checkCountConstraint(FileRuleDTO rule,
									 List<ExtractedEntry> entries,
									 Map<String, Object> ruleJson,
									 List<TaskValidateResponse.ValidationIssue> issues) {
		Integer min = firstInteger(ruleJson, "minCount", "min");
		Integer max = firstInteger(ruleJson, "maxCount", "max");
		if (min == null && max == null) {
			return true;
		}
		int count = entries.size();
		if (min != null && count < min || max != null && count > max) {
			issues.add(issue(rule.getFileKey(), rule.getPathPattern(), "PATH_COUNT_OUT_OF_RANGE", "路径数量不满足模板约束"));
			return false;
		}
		return true;
	}

	private void checkSuffixConstraint(FileRuleDTO rule,
								 List<ExtractedEntry> entries,
								 Map<String, Object> ruleJson,
								 List<TaskValidateResponse.ValidationIssue> issues) {
		Object raw = ruleJson.get("allowSuffix");
		if (!(raw instanceof List<?> suffixRows) || suffixRows.isEmpty()) {
			return;
		}
		Set<String> allowSuffix = suffixRows.stream()
				.filter(Objects::nonNull)
				.map(v -> String.valueOf(v).toLowerCase(Locale.ROOT))
				.collect(java.util.stream.Collectors.toSet());
		for (ExtractedEntry entry : entries) {
			if (entry.directory()) {
				continue;
			}
			if (!allowSuffix.contains(entry.suffix().toLowerCase(Locale.ROOT))) {
				issues.add(issue(rule.getFileKey(), entry.relativePath(), "INVALID_SUFFIX", "文件后缀不在允许范围"));
				return;
			}
		}
	}

	private Integer firstInteger(Map<String, Object> map, String... keys) {
		for (String key : keys) {
			Object value = map.get(key);
			if (value == null) {
				continue;
			}
			if (value instanceof Number number) {
				return number.intValue();
			}
			try {
				return Integer.parseInt(String.valueOf(value));
			} catch (NumberFormatException ignored) {
			}
		}
		return null;
	}

	private TaskValidateResponse.ValidationIssue issue(String ruleKey, String path, String errorCode, String message) {
		TaskValidateResponse.ValidationIssue issue = new TaskValidateResponse.ValidationIssue();
		issue.setRuleKey(ruleKey);
		issue.setPath(path);
		issue.setErrorCode(errorCode);
		issue.setMessage(message);
		return issue;
	}

	private String firstNonBlank(String first, String second) {
		if (!isBlank(first)) {
			return first.trim();
		}
		if (!isBlank(second)) {
			return second.trim();
		}
		return null;
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private boolean pathMatches(String pattern, String path) {
		String normalizedPattern = normalizeRelativePath(pattern);
		String normalizedPath = normalizeRelativePath(path);
		if (normalizedPattern.endsWith("/**")) {
			String base = normalizedPattern.substring(0, normalizedPattern.length() - 3);
			if (normalizedPath.equals(base)) {
				return true;
			}
		}
		return globMatch(normalizedPattern, normalizedPath);
	}

	private boolean globMatch(String glob, String value) {
		String normalizedGlob = normalizeRelativePath(glob);
		String normalizedValue = normalizeRelativePath(value);
		String regex = toRegex(normalizedGlob);
		return normalizedValue.matches(regex);
	}

	private String toRegex(String glob) {
		StringBuilder builder = new StringBuilder("^");
		for (int i = 0; i < glob.length(); i++) {
			char c = glob.charAt(i);
			if (c == '*') {
				if (i + 1 < glob.length() && glob.charAt(i + 1) == '*') {
					builder.append(".*");
					i++;
				} else {
					builder.append("[^/]*");
				}
			} else if (c == '?') {
				builder.append('.');
			} else {
				if (".[]{}()+-^$|\\".indexOf(c) >= 0) {
					builder.append('\\');
				}
				builder.append(c);
			}
		}
		builder.append('$');
		return builder.toString();
	}

	private String normalizeRelativePath(String path) {
		if (path == null) {
			return "";
		}
		String normalized = path.replace('\\', '/').trim();
		while (normalized.startsWith("/")) {
			normalized = normalized.substring(1);
		}
		if (normalized.startsWith("./")) {
			normalized = normalized.substring(2);
		}
		return normalized;
	}

	private String extractSuffix(String fileName) {
		int idx = fileName.lastIndexOf('.');
		return idx < 0 ? "" : fileName.substring(idx + 1);
	}

	private void cleanupDirectory(Path dir) throws IOException {
		if (!Files.exists(dir)) {
			return;
		}
		try (var stream = Files.walk(dir)) {
			stream.sorted(Comparator.reverseOrder()).forEach(path -> {
				try {
					Files.deleteIfExists(path);
				} catch (IOException ex) {
					throw new IllegalStateException("cleanup workdir failed", ex);
				}
			});
		}
	}

	private Task loadAndCheckOwner(Long taskId, Long userId) {
		Task task = taskRepository.findById(taskId).orElseThrow(() -> new BizException(ErrorCodeConstants.TASK_NOT_FOUND, "task not found"));
		if (!task.isOwner(userId)) {
			throw new BizException(ErrorCodeConstants.FORBIDDEN, "no permission");
		}
		return task;
	}

	private boolean shouldAttachValidationData(BizException ex) {
		return ex.getCode() != null && switch (ex.getCode()) {
			case ErrorCodeConstants.TASK_VALIDATION_FAILED,
					ErrorCodeConstants.TASK_STATUS_NOT_EDITABLE,
					ErrorCodeConstants.PROFILE_NOT_FOUND,
					ErrorCodeConstants.TASK_PROFILE_MISMATCH,
					ErrorCodeConstants.TASK_TYPE_MISMATCH -> true;
			default -> false;
		};
	}

	private TaskValidateResponse buildInvalidResponse(Task task, List<TaskValidateResponse.ValidationIssue> issues) {
		TaskValidateResponse response = new TaskValidateResponse();
		response.setTaskId(task.getId());
		response.setValid(Boolean.FALSE);
		response.setStatus(task.getStatus());
		response.setIssues(issues == null ? List.of() : issues);
		return response;
	}

	private record ExtractedContext(Path workDir, List<ExtractedEntry> entries) {
	}

	private record ExtractedEntry(String relativePath, boolean directory, String name, String suffix) {
	}
}
