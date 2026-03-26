package com.example.cae.nodeagent.infrastructure.support;

import com.example.cae.common.exception.BizException;
import com.example.cae.nodeagent.domain.model.ExecutionContext;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class CommandBuilder {
	public List<String> buildCommand(ExecutionContext context) {
		if (context.getCommandTemplate() == null || context.getCommandTemplate().trim().isEmpty()) {
			throw new BizException("commandTemplate is empty");
		}
		return Arrays.asList("cmd", "/c", context.getCommandTemplate());
	}
}

