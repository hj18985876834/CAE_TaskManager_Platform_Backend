package com.example.cae.nodeagent.domain.executor;

import com.example.cae.common.exception.BizException;
import com.example.cae.nodeagent.application.manager.TaskReportManager;
import com.example.cae.nodeagent.domain.model.ExecutionContext;
import com.example.cae.nodeagent.domain.model.ExecutionResult;
import com.example.cae.nodeagent.domain.parser.ResultParser;
import com.example.cae.nodeagent.domain.service.ResultParserSelectDomainService;
import com.example.cae.nodeagent.infrastructure.process.ProcessRunner;
import com.example.cae.nodeagent.infrastructure.storage.ResultFileCollector;
import com.example.cae.nodeagent.infrastructure.support.CommandBuilder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

@Component
@Order(100)
public class CommandTemplateExecutor extends AbstractSolverExecutor {
	private final CommandBuilder commandBuilder;
	private final ProcessRunner processRunner;
	private final ResultFileCollector resultFileCollector;
	private final TaskReportManager taskReportManager;
	private final ResultParserSelectDomainService resultParserSelectDomainService;

	public CommandTemplateExecutor(CommandBuilder commandBuilder,
								   ProcessRunner processRunner,
								   ResultFileCollector resultFileCollector,
								   TaskReportManager taskReportManager,
								   ResultParserSelectDomainService resultParserSelectDomainService) {
		this.commandBuilder = commandBuilder;
		this.processRunner = processRunner;
		this.resultFileCollector = resultFileCollector;
		this.taskReportManager = taskReportManager;
		this.resultParserSelectDomainService = resultParserSelectDomainService;
	}

	@Override
	public boolean supports(ExecutionContext context) {
		if (context == null || context.getCommandTemplate() == null || context.getCommandTemplate().isBlank()) {
			return false;
		}
		if ("MOCK".equalsIgnoreCase(context.getSolverCode())) {
			return false;
		}
		if (context.getSolverId() != null && context.getSolverId() > 0) {
			return true;
		}
		return context.getSolverCode() != null && !context.getSolverCode().isBlank();
	}

	@Override
	protected ExecutionResult doExecute(ExecutionContext context) {
		long start = System.currentTimeMillis();
		List<String> command = commandBuilder.buildCommand(context);
		int exitCode = processRunner.run(
				context.getTaskId(),
				command,
				resolveProcessWorkDir(context),
				context.getTimeoutSeconds(),
				line -> taskReportManager.pushLog(context.getTaskId(), null, line),
				line -> taskReportManager.pushLog(context.getTaskId(), null, line)
		);
		if (exitCode != 0) {
			throw new BizException("solver execute failed with exit code " + exitCode);
		}
		List<File> resultFiles = resultFileCollector.collect(context);
		ResultParser resultParser = resultParserSelectDomainService.select(context == null ? null : context.getParserName());
		return resultParser.parse(context, exitCode, start, resultFiles);
	}
}
