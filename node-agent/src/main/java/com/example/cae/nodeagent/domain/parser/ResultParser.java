package com.example.cae.nodeagent.domain.parser;

import com.example.cae.nodeagent.domain.model.ExecutionContext;
import com.example.cae.nodeagent.domain.model.ExecutionResult;

import java.io.File;
import java.util.List;

public interface ResultParser {
	boolean supports(String parserName);

	ExecutionResult parse(ExecutionContext context, int exitCode, long startMillis, List<File> resultFiles);
}
