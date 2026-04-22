package com.example.cae.task.infrastructure.client;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.exception.BizException;
import com.example.cae.common.response.Result;
import com.example.cae.task.config.TaskRemoteServiceProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class UserClient {
	private final RestTemplate restTemplate;
	private final String userServiceBaseUrl;

	public UserClient(RestTemplate restTemplate, TaskRemoteServiceProperties remoteServiceProperties) {
		this.restTemplate = restTemplate;
		this.userServiceBaseUrl = remoteServiceProperties.getUserBaseUrl();
	}

	public String getUsername(Long userId) {
		UserBasic userBasic = getUserBasic(userId);
		return userBasic == null ? null : userBasic.getUsername();
	}

	@SuppressWarnings("unchecked")
	public UserBasic getUserBasic(Long userId) {
		if (userId == null) {
			return null;
		}
		String url = userServiceBaseUrl + "/internal/users/" + userId;
		Result<?> result = restTemplate.getForObject(url, Result.class);
		ensureSuccess(result, "get internal user");
		if (!(result.getData() instanceof Map<?, ?> rawMap)) {
			throw new BizException(ErrorCodeConstants.BAD_GATEWAY, "get internal user response data is invalid");
		}
		Map<String, Object> map = (Map<String, Object>) rawMap;
		UserBasic userBasic = new UserBasic();
		userBasic.setId(toLong(map.get("id")));
		userBasic.setUsername(toString(map.get("username")));
		userBasic.setRealName(toString(map.get("realName")));
		userBasic.setStatus(toInteger(map.get("status")));
		return userBasic;
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

	public static class UserBasic {
		private Long id;
		private String username;
		private String realName;
		private Integer status;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getRealName() {
			return realName;
		}

		public void setRealName(String realName) {
			this.realName = realName;
		}

		public Integer getStatus() {
			return status;
		}

		public void setStatus(Integer status) {
			this.status = status;
		}
	}
}
