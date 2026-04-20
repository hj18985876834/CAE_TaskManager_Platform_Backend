package com.example.cae.task.infrastructure.client;

import com.example.cae.common.dto.FileRuleDTO;
import com.example.cae.common.response.Result;
import com.example.cae.task.config.TaskRemoteServiceProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class SolverClient {
	private final RestTemplate restTemplate;
	private final String solverServiceBaseUrl;

	public SolverClient(RestTemplate restTemplate, TaskRemoteServiceProperties remoteServiceProperties) {
		this.restTemplate = restTemplate;
		this.solverServiceBaseUrl = remoteServiceProperties.getSolverBaseUrl();
	}

	public Object getProfileDetail(Long profileId) {
		return getInternalProfileMap(profileId);
	}

	public Long getProfileSolverId(Long profileId) {
		Map<String, Object> profileMap = getInternalProfileMap(profileId);
		return profileMap == null ? null : toLong(profileMap.get("solverId"));
	}

	public String getProfileTaskType(Long profileId) {
		Map<String, Object> profileMap = getInternalProfileMap(profileId);
		return profileMap == null ? null : toString(profileMap.get("taskType"));
	}

	public String getProfileName(Long profileId) {
		Map<String, Object> profileMap = getInternalProfileMap(profileId);
		return profileMap == null ? null : toString(profileMap.get("profileName"));
	}

	public List<FileRuleDTO> getFileRules(Long profileId) {
		Map<String, Object> profileMap = getInternalProfileMap(profileId);
		if (profileMap == null || !(profileMap.get("fileRules") instanceof List<?> rows)) {
			return List.of();
		}
		List<FileRuleDTO> rules = new ArrayList<>();
		for (Object row : rows) {
			if (!(row instanceof Map<?, ?> map)) {
				continue;
			}
			FileRuleDTO dto = new FileRuleDTO();
			dto.setRuleId(toLong(map.get("ruleId")));
			dto.setProfileId(toLong(map.get("profileId")));
			dto.setFileKey(toString(map.get("fileKey")));
			dto.setPathPattern(toString(map.get("pathPattern")));
			dto.setFileNamePattern(toString(map.get("fileNamePattern")));
			dto.setFileType(toString(map.get("fileType")));
			dto.setRequiredFlag(toInteger(map.get("requiredFlag")));
			dto.setSortOrder(toInteger(map.get("sortOrder")));
			dto.setRuleJson(toString(map.get("ruleJson")));
			dto.setRemark(toString(map.get("remark")));
			rules.add(dto);
		}
		return rules;
	}

	public Object getUploadSpec(Long profileId) {
		String url = solverServiceBaseUrl + "/api/profiles/" + profileId + "/upload-spec";
		Result<?> result = restTemplate.getForObject(url, Result.class);
		return result == null ? null : result.getData();
	}

	@SuppressWarnings("unchecked")
	public UploadSpecMeta getUploadSpecMeta(Long profileId) {
		Object data = getUploadSpec(profileId);
		if (!(data instanceof Map<?, ?> dataMap)) {
			return null;
		}
		Map<String, Object> map = (Map<String, Object>) dataMap;
		UploadSpecMeta meta = new UploadSpecMeta();
		meta.setUploadMode(toString(map.get("uploadMode")));
		if (map.get("archiveRule") instanceof Map<?, ?> archiveRuleMapRaw) {
			Map<String, Object> archiveRuleMap = (Map<String, Object>) archiveRuleMapRaw;
			meta.setArchiveFileKey(toString(archiveRuleMap.get("fileKey")));
			meta.setMaxSizeMb(toInteger(archiveRuleMap.get("maxSizeMb")));
			if (archiveRuleMap.get("allowSuffix") instanceof List<?> suffixRows) {
				List<String> suffixes = new ArrayList<>();
				for (Object suffix : suffixRows) {
					if (suffix != null) {
						suffixes.add(String.valueOf(suffix));
					}
				}
				meta.setAllowSuffix(suffixes);
			}
		}
		return meta;
	}

	public String getSolverCode(Long solverId) {
		SolverMeta solverMeta = getSolverMeta(solverId);
		return solverMeta == null ? null : solverMeta.getSolverCode();
	}

	public String getSolverName(Long solverId) {
		SolverMeta solverMeta = getSolverMeta(solverId);
		return solverMeta == null ? null : solverMeta.getSolverName();
	}

	public SolverMeta getSolverMeta(Long solverId) {
		String url = solverServiceBaseUrl + "/internal/solvers/" + solverId;
		Result<?> result = restTemplate.getForObject(url, Result.class);
		if (result == null || !(result.getData() instanceof Map<?, ?> map)) {
			return null;
		}
		SolverMeta solverMeta = new SolverMeta();
		solverMeta.setSolverCode(toString(map.get("solverCode")));
		solverMeta.setSolverName(toString(map.get("solverName")));
		solverMeta.setExecMode(toString(map.get("execMode")));
		solverMeta.setExecPath(toString(map.get("execPath")));
		return solverMeta;
	}

	public ProfileExecutionMeta getProfileExecutionMeta(Long profileId) {
		Map<String, Object> map = getInternalProfileMap(profileId);
		if (map == null) {
			return null;
		}
		ProfileExecutionMeta meta = new ProfileExecutionMeta();
		meta.setCommandTemplate(toString(map.get("commandTemplate")));
		meta.setParserName(toString(map.get("parserName")));
		meta.setTimeoutSeconds(toInteger(map.get("timeoutSeconds")));
		return meta;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> getInternalProfileMap(Long profileId) {
		String url = solverServiceBaseUrl + "/internal/profiles/" + profileId;
		Result<?> result = restTemplate.getForObject(url, Result.class);
		if (result == null || !(result.getData() instanceof Map<?, ?> map)) {
			return null;
		}
		return (Map<String, Object>) map;
	}

	private String toString(Object value) {
		return value == null ? null : String.valueOf(value);
	}

	private Long toLong(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Number number) {
			return number.longValue();
		}
		return Long.parseLong(String.valueOf(value));
	}

	private Integer toInteger(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Number number) {
			return number.intValue();
		}
		return Integer.parseInt(String.valueOf(value));
	}

	public static class ProfileExecutionMeta {
		private String commandTemplate;
		private String parserName;
		private Integer timeoutSeconds;

		public String getCommandTemplate() {
			return commandTemplate;
		}

		public void setCommandTemplate(String commandTemplate) {
			this.commandTemplate = commandTemplate;
		}

		public String getParserName() {
			return parserName;
		}

		public void setParserName(String parserName) {
			this.parserName = parserName;
		}

		public Integer getTimeoutSeconds() {
			return timeoutSeconds;
		}

		public void setTimeoutSeconds(Integer timeoutSeconds) {
			this.timeoutSeconds = timeoutSeconds;
		}
	}

	public static class UploadSpecMeta {
		private String uploadMode;
		private String archiveFileKey;
		private Integer maxSizeMb;
		private List<String> allowSuffix;

		public String getUploadMode() {
			return uploadMode;
		}

		public void setUploadMode(String uploadMode) {
			this.uploadMode = uploadMode;
		}

		public String getArchiveFileKey() {
			return archiveFileKey;
		}

		public void setArchiveFileKey(String archiveFileKey) {
			this.archiveFileKey = archiveFileKey;
		}

		public Integer getMaxSizeMb() {
			return maxSizeMb;
		}

		public void setMaxSizeMb(Integer maxSizeMb) {
			this.maxSizeMb = maxSizeMb;
		}

		public List<String> getAllowSuffix() {
			return allowSuffix;
		}

		public void setAllowSuffix(List<String> allowSuffix) {
			this.allowSuffix = allowSuffix;
		}
	}

	public static class SolverMeta {
		private String solverCode;
		private String solverName;
		private String execMode;
		private String execPath;

		public String getSolverCode() {
			return solverCode;
		}

		public void setSolverCode(String solverCode) {
			this.solverCode = solverCode;
		}

		public String getSolverName() {
			return solverName;
		}

		public void setSolverName(String solverName) {
			this.solverName = solverName;
		}

		public String getExecMode() {
			return execMode;
		}

		public void setExecMode(String execMode) {
			this.execMode = execMode;
		}

		public String getExecPath() {
			return execPath;
		}

		public void setExecPath(String execPath) {
			this.execPath = execPath;
		}
	}
}
