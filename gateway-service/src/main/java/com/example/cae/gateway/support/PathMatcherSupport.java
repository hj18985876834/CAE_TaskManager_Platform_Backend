package com.example.cae.gateway.support;

import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.List;

@Component
public class PathMatcherSupport {
	private final AntPathMatcher antPathMatcher;

	public PathMatcherSupport(AntPathMatcher antPathMatcher) {
		this.antPathMatcher = antPathMatcher;
	}

	public boolean matchAny(String path, List<String> patterns) {
		if (patterns == null || patterns.isEmpty()) {
			return false;
		}
		return patterns.stream().anyMatch(pattern -> antPathMatcher.match(pattern, path));
	}
}