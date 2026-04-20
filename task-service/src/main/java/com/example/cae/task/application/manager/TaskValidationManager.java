package com.example.cae.task.application.manager;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.dto.FileRuleDTO;
import com.example.cae.common.enums.OperatorTypeEnum;
import com.example.cae.common.enums.TaskStatusEnum;
import com.example.cae.common.exception.BizException;
import com.example.cae.common.utils.JsonUtil;
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
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class TaskValidationManager {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final Set<String> RULE_METADATA_KEYS = Set.of(
            "allowSuffix",
            "minCount",
            "maxCount",
            "min",
            "max",
            "maxSizeMb",
            "deriveParam",
            "deriveParams"
    );

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

    @Transactional
    public TaskValidateResponse validateTask(Long taskId, Long userId) {
        Task task = loadAndCheckOwner(taskId, userId);
        List<TaskFile> files = taskFileRepository.listByTaskId(taskId);
        Long profileSolverId = solverClient.getProfileSolverId(task.getProfileId());
        String profileTaskType = solverClient.getProfileTaskType(task.getProfileId());
        SolverClient.SolverMeta solverMeta = solverClient.getSolverMeta(task.getSolverId());
        String solverCode = solverMeta == null ? null : solverMeta.getSolverCode();
        List<FileRuleDTO> rules = solverClient.getFileRules(task.getProfileId());
        List<TaskValidateResponse.ValidationIssue> issues = new ArrayList<>();
        Map<String, Object> taskParams = parseTaskParams(task.getParamsJson());
        ValidationOutcome validationOutcome = null;

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
            List<RuleDefinition> ruleDefinitions = buildRuleDefinitions(rules, issues);
            validationOutcome = validateArchiveAndRules(task, files, ruleDefinitions, solverMeta, taskParams, issues);
            if (!issues.isEmpty()) {
                throw new BizException(ErrorCodeConstants.TASK_VALIDATION_FAILED, "task file validation failed", buildInvalidResponse(task, issues));
            }
        } catch (BizException ex) {
            if (shouldAttachValidationData(ex)) {
                throw new BizException(ex.getCode(), ex.getMessage(), buildInvalidResponse(task, issues));
            }
            throw ex;
        }

        mergeDerivedParams(task, validationOutcome);
        if (!TaskStatusEnum.VALIDATED.name().equals(task.getStatus())) {
            taskStatusDomainService.transfer(task, TaskStatusEnum.VALIDATED.name(), "validation passed", OperatorTypeEnum.USER.name(), userId);
        }
        taskRepository.update(task);

        TaskValidateResponse response = new TaskValidateResponse();
        response.setTaskId(taskId);
        response.setValid(Boolean.TRUE);
        response.setStatus(task.getStatus());
        response.setIssues(List.of());
        return response;
    }

    private ValidationOutcome validateArchiveAndRules(Task task,
                                                      List<TaskFile> files,
                                                      List<RuleDefinition> ruleDefinitions,
                                                      SolverClient.SolverMeta solverMeta,
                                                      Map<String, Object> taskParams,
                                                      List<TaskValidateResponse.ValidationIssue> issues) {
        String archiveFileKey = resolveArchiveFileKey(ruleDefinitions);
        TaskFile archive = files.stream()
                .filter(file -> file.isArchiveFile() || archiveFileKey.equalsIgnoreCase(file.getFileKey()))
                .findFirst()
                .orElse(null);
        if (archive == null) {
            issues.add(issue(archiveFileKey, null, "ARCHIVE_MISSING", "Missing input archive"));
            return null;
        }
        String suffix = archive.getFileSuffix() == null ? "" : archive.getFileSuffix().toLowerCase(Locale.ROOT);
        if (!resolveAllowedSuffixes(ruleDefinitions).contains(suffix)) {
            issues.add(issue(archiveFileKey, archive.getOriginName(), "INVALID_ARCHIVE_SUFFIX", "Archive suffix is not allowed"));
            return null;
        }
        if (!"zip".equals(suffix)) {
            issues.add(issue(archiveFileKey, archive.getOriginName(), "UNSUPPORTED_ARCHIVE_FORMAT", "Only .zip archives are currently supported"));
            return null;
        }

        ExtractedContext extracted = extractArchive(task, archive, issues);
        if (extracted == null) {
            return null;
        }

        Map<String, Object> validationVariables = buildValidationVariables(task, solverMeta, taskParams);
        DerivationOutcome derivationOutcome = deriveValidationVariables(ruleDefinitions, extracted, validationVariables, issues);

        for (RuleDefinition ruleDefinition : ruleDefinitions) {
            if (shouldSkipExtractedRuleValidation(ruleDefinition.rule())) {
                continue;
            }
            if (!shouldApplyRule(ruleDefinition.ruleJson(), validationVariables)) {
                continue;
            }
            validateRule(ruleDefinition.rule(), extracted.entries(), ruleDefinition.ruleJson(), issues);
        }
        return new ValidationOutcome(derivationOutcome.derivedParams());
    }

    private List<RuleDefinition> buildRuleDefinitions(List<FileRuleDTO> rules, List<TaskValidateResponse.ValidationIssue> issues) {
        if (rules == null || rules.isEmpty()) {
            return List.of();
        }
        return rules.stream()
                .filter(Objects::nonNull)
                .map(rule -> new RuleDefinition(rule, parseRuleJson(rule, issues)))
                .filter(ruleDefinition -> ruleDefinition.ruleJson() != null)
                .sorted(Comparator.comparing(ruleDefinition -> ruleDefinition.rule().getSortOrder(), Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }

    private String resolveArchiveFileKey(List<RuleDefinition> ruleDefinitions) {
        return ruleDefinitions.stream()
                .map(RuleDefinition::rule)
                .filter(this::isArchiveRule)
                .map(FileRuleDTO::getFileKey)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse("input_archive");
    }

    private Set<String> resolveAllowedSuffixes(List<RuleDefinition> ruleDefinitions) {
        return ruleDefinitions.stream()
                .filter(ruleDefinition -> isArchiveRule(ruleDefinition.rule()))
                .map(RuleDefinition::ruleJson)
                .map(ruleJson -> ruleJson == null ? List.<String>of() : readStringList(ruleJson.get("allowSuffix")))
                .filter(values -> !values.isEmpty())
                .findFirst()
                .map(values -> values.stream()
                        .filter(Objects::nonNull)
                        .map(value -> value.toLowerCase(Locale.ROOT))
                        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)))
                .orElseGet(() -> new LinkedHashSet<>(List.of("zip")));
    }

    private Map<String, Object> buildValidationVariables(Task task,
                                                         SolverClient.SolverMeta solverMeta,
                                                         Map<String, Object> taskParams) {
        Map<String, Object> variables = new LinkedHashMap<>();
        if (taskParams != null && !taskParams.isEmpty()) {
            variables.putAll(taskParams);
        }
        if (task != null) {
            if (task.getSolverId() != null) {
                variables.put("solverId", task.getSolverId());
            }
            if (task.getProfileId() != null) {
                variables.put("profileId", task.getProfileId());
            }
            if (task.getTaskType() != null && !task.getTaskType().isBlank()) {
                variables.put("taskType", task.getTaskType());
            }
        }
        if (solverMeta != null) {
            if (solverMeta.getSolverCode() != null && !solverMeta.getSolverCode().isBlank()) {
                variables.put("solverCode", solverMeta.getSolverCode());
            }
            if (solverMeta.getExecMode() != null && !solverMeta.getExecMode().isBlank()) {
                variables.put("solverExecMode", solverMeta.getExecMode());
            }
            if (solverMeta.getExecPath() != null && !solverMeta.getExecPath().isBlank()) {
                variables.put("solverExecPath", solverMeta.getExecPath());
            }
        }
        return variables;
    }

    private DerivationOutcome deriveValidationVariables(List<RuleDefinition> ruleDefinitions,
                                                        ExtractedContext extracted,
                                                        Map<String, Object> validationVariables,
                                                        List<TaskValidateResponse.ValidationIssue> issues) {
        Map<String, Object> derivedParams = new LinkedHashMap<>();
        for (RuleDefinition ruleDefinition : ruleDefinitions) {
            if (!shouldApplyRule(ruleDefinition.ruleJson(), validationVariables)) {
                continue;
            }
            List<Map<String, Object>> deriveConfigs = readDeriveConfigs(ruleDefinition.ruleJson());
            if (deriveConfigs.isEmpty()) {
                continue;
            }
            List<ExtractedEntry> matchedEntries = matchEntries(ruleDefinition.rule(), extracted.entries());
            for (Map<String, Object> deriveConfig : deriveConfigs) {
                tryDeriveParam(ruleDefinition.rule(), deriveConfig, extracted.caseDir(), matchedEntries, validationVariables, derivedParams, issues);
            }
        }
        return new DerivationOutcome(derivedParams);
    }

    private List<Map<String, Object>> readDeriveConfigs(Map<String, Object> ruleJson) {
        if (ruleJson == null || ruleJson.isEmpty()) {
            return List.of();
        }
        Object rawSingle = ruleJson.get("deriveParam");
        if (rawSingle instanceof Map<?, ?> row) {
            return List.of(stringKeyMap(row));
        }
        Object rawList = ruleJson.get("deriveParams");
        if (!(rawList instanceof List<?> rows) || rows.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> deriveConfigs = new ArrayList<>();
        for (Object row : rows) {
            if (row instanceof Map<?, ?> map) {
                deriveConfigs.add(stringKeyMap(map));
            }
        }
        return deriveConfigs;
    }

    private void tryDeriveParam(FileRuleDTO rule,
                                Map<String, Object> deriveConfig,
                                Path caseDir,
                                List<ExtractedEntry> matchedEntries,
                                Map<String, Object> validationVariables,
                                Map<String, Object> derivedParams,
                                List<TaskValidateResponse.ValidationIssue> issues) {
        String paramName = trimToNull(deriveConfig.get("name"));
        if (paramName == null) {
            issues.add(issue(rule.getFileKey(), rule.getPathPattern(), "DERIVED_PARAM_CONFIG_INVALID", "deriveParam.name is required"));
            return;
        }
        Object derivedValue = resolveDerivedValue(rule, deriveConfig, caseDir, matchedEntries, issues);
        if (derivedValue == null) {
            return;
        }
        Object existingValue = validationVariables.get(paramName);
        if (existingValue != null && !sameValue(existingValue, derivedValue)) {
            issues.add(issue(rule.getFileKey(), rule.getPathPattern(), "DERIVED_PARAM_CONFLICT", "Derived parameter conflicts with task parameter: " + paramName));
            return;
        }
        validationVariables.put(paramName, derivedValue);
        derivedParams.put(paramName, derivedValue);
    }

    private Object resolveDerivedValue(FileRuleDTO rule,
                                       Map<String, Object> deriveConfig,
                                       Path caseDir,
                                       List<ExtractedEntry> matchedEntries,
                                       List<TaskValidateResponse.ValidationIssue> issues) {
        String source = trimToNull(deriveConfig.get("source"));
        if (source == null) {
            source = "fileContentRegex";
        }
        return switch (source) {
            case "literal" -> deriveLiteralValue(rule, deriveConfig, issues);
            case "fileNameRegex" -> deriveRegexValue(rule, deriveConfig, matchedEntries, ExtractedEntry::name, issues);
            case "relativePathRegex" -> deriveRegexValue(rule, deriveConfig, matchedEntries, ExtractedEntry::relativePath, issues);
            case "fileContentRegex" -> deriveFileContentValue(rule, deriveConfig, caseDir, matchedEntries, issues);
            default -> {
                issues.add(issue(rule.getFileKey(), rule.getPathPattern(), "DERIVED_PARAM_SOURCE_UNSUPPORTED", "Unsupported deriveParam.source: " + source));
                yield null;
            }
        };
    }

    private Object deriveLiteralValue(FileRuleDTO rule,
                                      Map<String, Object> deriveConfig,
                                      List<TaskValidateResponse.ValidationIssue> issues) {
        Object value = deriveConfig.get("value");
        if (value == null) {
            issues.add(issue(rule.getFileKey(), rule.getPathPattern(), "DERIVED_PARAM_CONFIG_INVALID", "deriveParam.value is required for literal source"));
            return null;
        }
        return normalizeDerivedValue(rule, deriveConfig, String.valueOf(value), issues);
    }

    private Object deriveRegexValue(FileRuleDTO rule,
                                    Map<String, Object> deriveConfig,
                                    List<ExtractedEntry> matchedEntries,
                                    java.util.function.Function<ExtractedEntry, String> valueExtractor,
                                    List<TaskValidateResponse.ValidationIssue> issues) {
        List<ExtractedEntry> fileEntries = matchedEntries.stream()
                .filter(entry -> !entry.directory())
                .toList();
        if (fileEntries.isEmpty()) {
            if (isRequiredDeriveConfig(deriveConfig)) {
                issues.add(issue(rule.getFileKey(), rule.getPathPattern(), "DERIVED_PARAM_SOURCE_MISSING", "No file matched for parameter derivation"));
            }
            return null;
        }
        String pattern = trimToNull(deriveConfig.get("pattern"));
        if (pattern == null) {
            issues.add(issue(rule.getFileKey(), rule.getPathPattern(), "DERIVED_PARAM_CONFIG_INVALID", "deriveParam.pattern is required"));
            return null;
        }
        Integer group = firstInteger(deriveConfig, "group");
        int groupIndex = group == null || group < 0 ? 1 : group;
        for (ExtractedEntry entry : fileEntries) {
            String rawValue = valueExtractor.apply(entry);
            if (rawValue == null) {
                continue;
            }
            try {
                Matcher matcher = Pattern.compile(pattern).matcher(rawValue);
                if (!matcher.find()) {
                    continue;
                }
                return normalizeDerivedValue(rule, deriveConfig, matcher.group(groupIndex), issues);
            } catch (Exception ex) {
                issues.add(issue(rule.getFileKey(), entry.relativePath(), "DERIVED_PARAM_PATTERN_INVALID", "deriveParam.pattern is invalid"));
                return null;
            }
        }
        if (isRequiredDeriveConfig(deriveConfig)) {
            issues.add(issue(rule.getFileKey(), rule.getPathPattern(), "DERIVED_PARAM_MATCH_FAILED", "Failed to derive parameter from matched files"));
        }
        return null;
    }

    private Object deriveFileContentValue(FileRuleDTO rule,
                                          Map<String, Object> deriveConfig,
                                          Path caseDir,
                                          List<ExtractedEntry> matchedEntries,
                                          List<TaskValidateResponse.ValidationIssue> issues) {
        List<ExtractedEntry> fileEntries = matchedEntries.stream()
                .filter(entry -> !entry.directory())
                .toList();
        if (fileEntries.isEmpty()) {
            if (isRequiredDeriveConfig(deriveConfig)) {
                issues.add(issue(rule.getFileKey(), rule.getPathPattern(), "DERIVED_PARAM_SOURCE_MISSING", "No file matched for parameter derivation"));
            }
            return null;
        }
        String pattern = trimToNull(deriveConfig.get("pattern"));
        if (pattern == null) {
            issues.add(issue(rule.getFileKey(), rule.getPathPattern(), "DERIVED_PARAM_CONFIG_INVALID", "deriveParam.pattern is required"));
            return null;
        }
        Integer group = firstInteger(deriveConfig, "group");
        int groupIndex = group == null || group < 0 ? 1 : group;
        for (ExtractedEntry entry : fileEntries) {
            Path filePath = caseDir.resolve(entry.relativePath()).normalize();
            String content;
            try {
                content = Files.readString(filePath);
            } catch (IOException ex) {
                issues.add(issue(rule.getFileKey(), entry.relativePath(), "DERIVED_PARAM_SOURCE_UNREADABLE", "Failed to read file for parameter derivation"));
                return null;
            }
            content = applyPreprocess(content, rule, deriveConfig, entry, issues);
            if (content == null) {
                return null;
            }
            try {
                Matcher matcher = Pattern.compile(pattern).matcher(content);
                if (!matcher.find()) {
                    continue;
                }
                return normalizeDerivedValue(rule, deriveConfig, matcher.group(groupIndex), issues);
            } catch (Exception ex) {
                issues.add(issue(rule.getFileKey(), entry.relativePath(), "DERIVED_PARAM_PATTERN_INVALID", "deriveParam.pattern is invalid"));
                return null;
            }
        }
        if (isRequiredDeriveConfig(deriveConfig)) {
            issues.add(issue(rule.getFileKey(), rule.getPathPattern(), "DERIVED_PARAM_MATCH_FAILED", "Failed to derive parameter from file content"));
        }
        return null;
    }

    private String applyPreprocess(String content,
                                   FileRuleDTO rule,
                                   Map<String, Object> deriveConfig,
                                   ExtractedEntry entry,
                                   List<TaskValidateResponse.ValidationIssue> issues) {
        Object rawPreprocess = deriveConfig.get("preprocess");
        if (!(rawPreprocess instanceof List<?> preprocessRows) || preprocessRows.isEmpty()) {
            return content;
        }
        String processed = content;
        for (Object preprocessRow : preprocessRows) {
            if (!(preprocessRow instanceof Map<?, ?> preprocessMapRaw)) {
                continue;
            }
            Map<String, Object> preprocessMap = stringKeyMap(preprocessMapRaw);
            String pattern = trimToNull(preprocessMap.get("pattern"));
            String replacement = preprocessMap.get("replacement") == null ? "" : String.valueOf(preprocessMap.get("replacement"));
            if (pattern == null) {
                continue;
            }
            try {
                processed = processed.replaceAll(pattern, replacement);
            } catch (Exception ex) {
                issues.add(issue(rule.getFileKey(), entry.relativePath(), "DERIVED_PARAM_PREPROCESS_INVALID", "deriveParam preprocess pattern is invalid"));
                return null;
            }
        }
        return processed;
    }

    private Object normalizeDerivedValue(FileRuleDTO rule,
                                         Map<String, Object> deriveConfig,
                                         String rawValue,
                                         List<TaskValidateResponse.ValidationIssue> issues) {
        if (rawValue == null) {
            return null;
        }
        String value = rawValue;
        Object trim = deriveConfig.get("trim");
        if (!(trim instanceof Boolean trimFlag) || trimFlag) {
            value = value.trim();
        }
        if (Boolean.TRUE.equals(deriveConfig.get("stripQuotes")) && value.length() >= 2) {
            if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                value = value.substring(1, value.length() - 1).trim();
            }
        }
        String sanitizeRegex = trimToNull(deriveConfig.get("sanitizeRegex"));
        if (sanitizeRegex != null) {
            try {
                if (!Pattern.compile(sanitizeRegex).matcher(value).matches()) {
                    issues.add(issue(rule.getFileKey(), rule.getPathPattern(), "DERIVED_PARAM_INVALID", "Derived parameter failed validation"));
                    return null;
                }
            } catch (Exception ex) {
                issues.add(issue(rule.getFileKey(), rule.getPathPattern(), "DERIVED_PARAM_CONFIG_INVALID", "deriveParam.sanitizeRegex is invalid"));
                return null;
            }
        }
        return value;
    }

    private boolean isRequiredDeriveConfig(Map<String, Object> deriveConfig) {
        Object required = deriveConfig.get("required");
        if (required instanceof Boolean flag) {
            return flag;
        }
        return required != null && Boolean.parseBoolean(String.valueOf(required));
    }

    private Map<String, Object> stringKeyMap(Map<?, ?> rawMap) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() != null) {
                normalized.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return normalized;
    }

    private boolean sameValue(Object left, Object right) {
        if (left == null || right == null) {
            return Objects.equals(left, right);
        }
        return String.valueOf(left).trim().equalsIgnoreCase(String.valueOf(right).trim());
    }

    private boolean shouldSkipExtractedRuleValidation(FileRuleDTO rule) {
        if (rule == null) {
            return true;
        }
        return isArchiveRule(rule);
    }

    private boolean isArchiveRule(FileRuleDTO rule) {
        if (rule == null) {
            return false;
        }
        if ("input_archive".equalsIgnoreCase(rule.getFileKey())) {
            return true;
        }
        return !isBlank(rule.getFileType()) && "ZIP".equalsIgnoreCase(rule.getFileType());
    }

    private ExtractedContext extractArchive(Task task, TaskFile archive, List<TaskValidateResponse.ValidationIssue> issues) {
        Path archivePath = Path.of(taskStoragePathSupport.toAbsoluteTaskPath(archive.getStoragePath())).normalize();
        Path workDir = Path.of(taskPathResolver.resolveTaskRoot(task.getId()), "workdir").normalize();
        ArchiveLayout archiveLayout = new ArchiveLayout();
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
                    archiveLayout.record(entryName, entry.isDirectory());
                    Path target = workDir.resolve(entryName).normalize();
                    if (!target.startsWith(workDir)) {
                        issues.add(issue(archive.getFileKey(), entry.getName(), "ARCHIVE_UNSAFE_PATH", "Archive contains an unsafe path"));
                        return null;
                    }
                    if (entry.isDirectory()) {
                        Files.createDirectories(target);
                    } else {
                        if (target.getParent() != null) {
                            Files.createDirectories(target.getParent());
                        }
                        Files.copy(zipInputStream, target);
                    }
                }
            }
            Path caseDir = archiveLayout.resolveCaseDir(workDir);
            archive.setUnpackDir(taskStoragePathSupport.toStoredTaskPath(caseDir.toString()));
            archive.setArchiveFlag(1);
            taskFileRepository.update(archive);
            return new ExtractedContext(workDir, caseDir, scanEntries(caseDir));
        } catch (IOException ex) {
            issues.add(issue(archive.getFileKey(), archive.getOriginName(), "ARCHIVE_BROKEN", "Archive is broken or cannot be extracted"));
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

    private void validateRule(FileRuleDTO rule,
                              List<ExtractedEntry> entries,
                              Map<String, Object> ruleJson,
                              List<TaskValidateResponse.ValidationIssue> issues) {
        List<ExtractedEntry> patternMatched = matchEntries(rule, entries);
        List<ExtractedEntry> typedMatched = applyTypeFilter(patternMatched, rule.getFileType());
        List<ExtractedEntry> namedMatched = applyNameFilter(typedMatched, rule.getFileNamePattern());
        String pathPattern = firstNonBlank(rule.getPathPattern(), rule.getFileNamePattern());
        if (pathPattern == null) {
            pathPattern = rule.getFileKey();
        }

        if (rule.getRequiredFlag() != null && rule.getRequiredFlag() == 1) {
            if (patternMatched.isEmpty()) {
                issues.add(issue(rule.getFileKey(), pathPattern, "MISSING_REQUIRED_PATH", "Missing required path: " + pathPattern));
                return;
            }
            if (!typedMatched.isEmpty() || !isBlank(rule.getFileType())) {
                if (typedMatched.isEmpty()) {
                    issues.add(issue(rule.getFileKey(), pathPattern, "INVALID_FILE_TYPE", "Path type does not match rule definition"));
                    return;
                }
            }
            if (!isBlank(rule.getFileNamePattern()) && namedMatched.isEmpty()) {
                issues.add(issue(rule.getFileKey(), pathPattern, "INVALID_FILENAME_PATTERN", "Filename does not match rule: " + rule.getFileNamePattern()));
                return;
            }
        }

        if (!checkCountConstraint(rule, namedMatched, ruleJson, issues)) {
            return;
        }
        checkSuffixConstraint(rule, namedMatched, ruleJson, issues);
    }

    private List<ExtractedEntry> matchEntries(FileRuleDTO rule, List<ExtractedEntry> entries) {
        if (rule == null || entries == null || entries.isEmpty()) {
            return List.of();
        }
        String pathPattern = firstNonBlank(rule.getPathPattern(), rule.getFileNamePattern());
        if (pathPattern == null) {
            return List.of();
        }
        return entries.stream()
                .filter(entry -> pathMatches(pathPattern, entry.relativePath()))
                .toList();
    }

    private boolean shouldApplyRule(Map<String, Object> ruleJson, Map<String, Object> validationVariables) {
        if (ruleJson == null || ruleJson.isEmpty()) {
            return true;
        }
        for (Map.Entry<String, Object> entry : ruleJson.entrySet()) {
            String variableName = resolveScopeVariableName(entry.getKey(), validationVariables);
            if (variableName == null) {
                continue;
            }
            List<String> candidates = readStringList(entry.getValue());
            if (candidates.isEmpty()) {
                continue;
            }
            Object actualValue = validationVariables.get(variableName);
            if (actualValue == null || String.valueOf(actualValue).isBlank()) {
                return false;
            }
            boolean matched = candidates.stream()
                    .anyMatch(candidate -> String.valueOf(actualValue).equalsIgnoreCase(candidate));
            if (!matched) {
                return false;
            }
        }
        return true;
    }

    private String resolveScopeVariableName(String ruleKey, Map<String, Object> validationVariables) {
        if (ruleKey == null || RULE_METADATA_KEYS.contains(ruleKey) || validationVariables == null || validationVariables.isEmpty()) {
            return null;
        }
        if (validationVariables.containsKey(ruleKey)) {
            return ruleKey;
        }
        if (ruleKey.endsWith("s")) {
            String singular = ruleKey.substring(0, ruleKey.length() - 1);
            if (validationVariables.containsKey(singular)) {
                return singular;
            }
        }
        return null;
    }

    private List<String> readStringList(Object raw) {
        if (raw == null) {
            return List.of();
        }
        if (raw instanceof List<?> rows) {
            return rows.stream()
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .toList();
        }
        String value = String.valueOf(raw).trim();
        if (value.isBlank()) {
            return List.of();
        }
        if (value.contains(",")) {
            return Arrays.stream(value.split(","))
                    .map(String::trim)
                    .filter(item -> !item.isBlank())
                    .toList();
        }
        return List.of(value);
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
            issues.add(issue(rule.getFileKey(), rule.getPathPattern(), "TEMPLATE_RULE_CONFLICT", "Rule JSON is invalid"));
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
            issues.add(issue(rule.getFileKey(), rule.getPathPattern(), "PATH_COUNT_OUT_OF_RANGE", "Matched path count is out of range"));
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
                .map(value -> String.valueOf(value).toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet());
        for (ExtractedEntry entry : entries) {
            if (entry.directory()) {
                continue;
            }
            if (!allowSuffix.contains(entry.suffix().toLowerCase(Locale.ROOT))) {
                issues.add(issue(rule.getFileKey(), entry.relativePath(), "INVALID_SUFFIX", "File suffix is not allowed"));
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
                // ignore and continue
            }
        }
        return null;
    }

    private void mergeDerivedParams(Task task, ValidationOutcome validationOutcome) {
        if (validationOutcome == null || validationOutcome.derivedParams() == null || validationOutcome.derivedParams().isEmpty()) {
            return;
        }
        Map<String, Object> params = parseTaskParams(task.getParamsJson());
        params.putAll(validationOutcome.derivedParams());
        task.setParamsJson(JsonUtil.toJson(params));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseTaskParams(String paramsJson) {
        if (paramsJson == null || paramsJson.isBlank()) {
            return new LinkedHashMap<>();
        }
        Object parsed;
        try {
            parsed = JsonUtil.fromJson(paramsJson, Map.class);
        } catch (Exception ex) {
            return new LinkedHashMap<>();
        }
        if (!(parsed instanceof Map<?, ?> rawMap)) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> params = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() != null) {
                params.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return params;
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

    private String trimToNull(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? null : text;
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

    private record ValidationOutcome(Map<String, Object> derivedParams) {
    }

    private record DerivationOutcome(Map<String, Object> derivedParams) {
    }

    private record RuleDefinition(FileRuleDTO rule, Map<String, Object> ruleJson) {
    }

    private record ExtractedContext(Path workDir, Path caseDir, List<ExtractedEntry> entries) {
    }

    private record ExtractedEntry(String relativePath, boolean directory, String name, String suffix) {
    }

    private static final class ArchiveLayout {
        private final Set<String> topLevelDirs = new LinkedHashSet<>();
        private boolean hasTopLevelFile;

        private void record(String entryName, boolean directory) {
            String normalized = entryName == null ? "" : entryName.replace("\\", "/");
            if (normalized.isBlank()) {
                return;
            }
            int idx = normalized.indexOf('/');
            if (idx < 0) {
                if (!directory) {
                    hasTopLevelFile = true;
                }
                return;
            }
            topLevelDirs.add(normalized.substring(0, idx));
        }

        private Path resolveCaseDir(Path workDir) {
            if (!hasTopLevelFile && topLevelDirs.size() == 1) {
                return workDir.resolve(topLevelDirs.iterator().next()).normalize();
            }
            return workDir;
        }
    }
}
