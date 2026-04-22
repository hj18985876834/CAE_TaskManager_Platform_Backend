package com.example.cae.scheduler.infrastructure.client.impl;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.exception.BizException;
import com.example.cae.common.response.Result;
import com.example.cae.scheduler.config.SchedulerRemoteServiceProperties;
import com.example.cae.scheduler.infrastructure.client.SolverClient;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class SolverClientStub implements SolverClient {
	private final RestTemplate restTemplate;
	private final String solverServiceBaseUrl;

	public SolverClientStub(RestTemplate restTemplate, SchedulerRemoteServiceProperties remoteServiceProperties) {
		this.restTemplate = restTemplate;
		this.solverServiceBaseUrl = remoteServiceProperties.getSolverBaseUrl();
	}

	@Override
	public SolverMeta getSolverMeta(Long solverId) {
		String url = solverServiceBaseUrl + "/internal/solvers/" + solverId;
		Result<?> result = restTemplate.getForObject(url, Result.class);
		ensureSuccess(result, "get solver meta");
		if (result == null || !(result.getData() instanceof Map<?, ?> rawMap)) {
			return null;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) rawMap;
		SolverMeta meta = new SolverMeta();
		meta.setSolverId(toLong(map.get("solverId")));
		meta.setSolverCode(toString(map.get("solverCode")));
		meta.setSolverName(toString(map.get("solverName")));
		meta.setEnabled(toInteger(map.get("enabled")));
		return meta;
	}

	@Override
	public ProfileMeta getProfileMeta(Long profileId) {
		String url = solverServiceBaseUrl + "/internal/profiles/" + profileId;
		Result<?> result = restTemplate.getForObject(url, Result.class);
		ensureSuccess(result, "get profile meta");
		if (result == null || !(result.getData() instanceof Map<?, ?> rawMap)) {
			return null;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) rawMap;
		ProfileMeta meta = new ProfileMeta();
		meta.setProfileId(toLong(map.get("profileId")));
		meta.setSolverId(toLong(map.get("solverId")));
		meta.setProfileCode(toString(map.get("profileCode")));
		meta.setProfileName(toString(map.get("profileName")));
		meta.setEnabled(toInteger(map.get("enabled")));
		return meta;
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

	private void ensureSuccess(Result<?> result, String action) {
		if (result == null) {
			throw new BizException(ErrorCodeConstants.BAD_GATEWAY, action + " response is empty");
		}
		if (result.getCode() != null && result.getCode() != 0) {
			throw new BizException(result.getCode(), result.getMessage(), result.getData());
		}
	}
}
