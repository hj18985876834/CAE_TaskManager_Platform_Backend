package com.example.cae.nodeagent.domain.parser;

import com.example.cae.nodeagent.domain.model.ExecutionContext;
import com.example.cae.nodeagent.domain.model.ExecutionResult;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class GenericResultParser implements ResultParser {
	@Override
	public boolean supports(String parserName) {
		return true;
	}

	@Override
	public ExecutionResult parse(ExecutionContext context, int exitCode, long startMillis, List<File> resultFiles) {
		int duration = (int) ((System.currentTimeMillis() - startMillis) / 1000);
		Map<String, Object> metrics = new LinkedHashMap<>();
		metrics.put("exitCode", exitCode);
		metrics.put("resultFileCount", resultFiles == null ? 0 : resultFiles.size());
		if (context != null && context.getParserName() != null && !context.getParserName().isBlank()) {
			metrics.put("parserName", context.getParserName());
		}
		return ExecutionResult.success(duration, buildSummary(context), metrics, resultFiles);
	}

	private String buildSummary(ExecutionContext context) {
		if (context != null && context.getParserName() != null && !context.getParserName().isBlank()) {
			return context.getParserName() + " parse success";
		}
		String solverCode = context == null ? null : context.getSolverCode();
		if (solverCode == null || solverCode.isBlank()) {
			return "solver execute success";
		}
		return solverCode.toLowerCase(Locale.ROOT) + " execute success";
	}
}
