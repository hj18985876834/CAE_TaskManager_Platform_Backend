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
			dto.setFileNamePattern(toString(map.get("fileNamePattern")));
			dto.setFileType(toString(map.get("fileType")));
			dto.setRequiredFlag(toInteger(map.get("requiredFlag")));
			dto.setSortOrder(toInteger(map.get("sortOrder")));
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

	public String getSolverCode(Long solverId) {
		String url = solverServiceBaseUrl + "/internal/solvers/" + solverId;
		Result<?> result = restTemplate.getForObject(url, Result.class);
		if (result == null || !(result.getData() instanceof Map<?, ?> map)) {
			return null;
		}
		return toString(map.get("solverCode"));
	}

	public String getSolverName(Long solverId) {
		String url = solverServiceBaseUrl + "/internal/solvers/" + solverId;
		Result<?> result = restTemplate.getForObject(url, Result.class);
		if (result == null || !(result.getData() instanceof Map<?, ?> map)) {
			return null;
		}
		return toString(map.get("solverName"));
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
}
