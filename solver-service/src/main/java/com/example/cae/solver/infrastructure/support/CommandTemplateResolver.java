package com.example.cae.solver.infrastructure.support;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CommandTemplateResolver {
	public String resolve(String commandTemplate, Map<String, Object> params) {
		String result = commandTemplate;
		if (result == null) {
			return null;
		}
		if (params == null || params.isEmpty()) {
			return result;
		}
		for (Map.Entry<String, Object> entry : params.entrySet()) {
			result = result.replace("${" + entry.getKey() + "}", String.valueOf(entry.getValue()));
		}
		return result;
	}
}

