package com.example.cae.gateway.support;

import com.example.cae.common.constant.HeaderConstants;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

@Component
public class GatewayRequestMutator {
	public ServerHttpRequest withUserContext(ServerHttpRequest request, Long userId, String roleCode) {
		return request.mutate()
				.header(HeaderConstants.X_USER_ID, String.valueOf(userId))
				.header(HeaderConstants.X_ROLE_CODE, roleCode)
				.build();
	}
}