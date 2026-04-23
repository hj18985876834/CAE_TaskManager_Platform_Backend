package com.example.cae.task.infrastructure.client;

import com.example.cae.common.dto.FileRuleDTO;
import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.exception.BizException;
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

	public ProfileMeta getProfileMeta(Long profileId) {
		Map<String, Object> profileMap = getInternalProfileMap(profileId);
		if (profileMap == null) {
			return null;
		}
		ProfileMeta meta = new ProfileMeta();
		meta.setProfileId(profileId);
		meta.setSolverId(toLong(profileMap.get("solverId")));
		meta.setTaskType(toString(profileMap.get("taskType")));
		meta.setProfileName(toString(profileMap.get("profileName")));
		meta.setEnabled(toInteger(profileMap.get("enabled")));
		return meta;
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

	public String getProfileParamsSchema(Long profileId) {
		Map<String, Object> profileMap = getInternalProfileMap(profileId);
		if (profileMap == null) {
			return null;
		}
		String paramsSchema = toString(profileMap.get("paramsSchema"));
		if (paramsSchema != null && !paramsSchema.isBlank()) {
			return paramsSchema;
		}
		return toString(profileMap.get("paramsSchemaJson"));
	}

	public List<FileRuleDTO> getFileRules(Long profileId) {
		Map<String, Object> profileMap = getInternalProfileMap(profileId);
		Object rowsObject = profileMap.get("fileRules");
		if (!(rowsObject instanceof List<?> rows)) {
			throw new BizException(ErrorCodeConstants.BAD_GATEWAY, "get internal profile fileRules response is invalid");
		}
		List<FileRuleDTO> rules = new ArrayList<>();
		for (Object row : rows) {
			if (!(row instanceof Map<?, ?> map)) {
				throw new BizException(ErrorCodeConstants.BAD_GATEWAY, "get internal profile fileRules row is invalid");
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
			dto.setDescription(toString(map.get("description")));
			rules.add(dto);
		}
		return rules;
	}

	public Object getUploadSpec(Long profileId) {
		String url = solverServiceBaseUrl + "/api/profiles/" + profileId + "/upload-spec";
		Result<?> result = restTemplate.getForObject(url, Result.class);
		ensureSuccess(result, "get upload spec");
		return result == null ? null : result.getData();
	}

	@SuppressWarnings("unchecked")
	public UploadSpecMeta getUploadSpecMeta(Long profileId) {
		Object data = getUploadSpec(profileId);
		if (!(data instanceof Map<?, ?> dataMap)) {
			throw new BizException(ErrorCodeConstants.BAD_GATEWAY, "get upload spec response data is invalid");
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
		ensureSuccess(result, "get solver meta");
		if (!(result.getData() instanceof Map<?, ?> map)) {
			throw new BizException(ErrorCodeConstants.BAD_GATEWAY, "get solver meta response data is invalid");
		}
		SolverMeta solverMeta = new SolverMeta();
		solverMeta.setSolverCode(toString(map.get("solverCode")));
		solverMeta.setSolverName(toString(map.get("solverName")));
		solverMeta.setExecMode(toString(map.get("execMode")));
		solverMeta.setExecPath(toString(map.get("execPath")));
		solverMeta.setEnabled(toInteger(map.get("enabled")));
		return solverMeta;
	}

	public ProfileExecutionMeta getProfileExecutionMeta(Long profileId) {
		Map<String, Object> map = getInternalProfileMap(profileId);
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
		ensureSuccess(result, "get internal profile");
		if (!(result.getData() instanceof Map<?, ?> map)) {
			throw new BizException(ErrorCodeConstants.BAD_GATEWAY, "get internal profile response data is invalid");
		}
		return (Map<String, Object>) map;
	}

	private void ensureSuccess(Result<?> result, String action) {
		if (result == null) {
			throw new BizException(ErrorCodeConstants.BAD_GATEWAY, action + " response is empty");
		}
		if (result.getCode() != null && result.getCode() != 0) {
			throw new BizException(result.getCode(), result.getMessage(), result.getData());
		}
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
		private Integer enabled;

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

		public Integer getEnabled() {
			return enabled;
		}

		public void setEnabled(Integer enabled) {
			this.enabled = enabled;
		}
	}

	public static class ProfileMeta {
		private Long profileId;
		private Long solverId;
		private String taskType;
		private String profileName;
		private Integer enabled;

		public Long getProfileId() {
			return profileId;
		}

		public void setProfileId(Long profileId) {
			this.profileId = profileId;
		}

		public Long getSolverId() {
			return solverId;
		}

		public void setSolverId(Long solverId) {
			this.solverId = solverId;
		}

		public String getTaskType() {
			return taskType;
		}

		public void setTaskType(String taskType) {
			this.taskType = taskType;
		}

		public String getProfileName() {
			return profileName;
		}

		public void setProfileName(String profileName) {
			this.profileName = profileName;
		}

		public Integer getEnabled() {
			return enabled;
		}

		public void setEnabled(Integer enabled) {
			this.enabled = enabled;
		}
	}
}
