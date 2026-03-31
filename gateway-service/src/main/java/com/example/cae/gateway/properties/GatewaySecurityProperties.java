package com.example.cae.gateway.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "gateway.security")
public class GatewaySecurityProperties {
	private List<String> whiteList;
	private List<String> adminOnlyPaths;
	private List<String> adminWritePaths;

	public List<String> getWhiteList() {
		return whiteList;
	}

	public void setWhiteList(List<String> whiteList) {
		this.whiteList = whiteList;
	}

	public List<String> getAdminOnlyPaths() {
		return adminOnlyPaths;
	}

	public void setAdminOnlyPaths(List<String> adminOnlyPaths) {
		this.adminOnlyPaths = adminOnlyPaths;
	}

	public List<String> getAdminWritePaths() {
		return adminWritePaths;
	}

	public void setAdminWritePaths(List<String> adminWritePaths) {
		this.adminWritePaths = adminWritePaths;
	}
}
