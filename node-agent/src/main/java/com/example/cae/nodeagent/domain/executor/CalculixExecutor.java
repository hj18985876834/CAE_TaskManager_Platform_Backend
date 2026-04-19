package com.example.cae.nodeagent.domain.executor;

import com.example.cae.common.exception.BizException;
import com.example.cae.nodeagent.application.manager.TaskReportManager;
import com.example.cae.nodeagent.domain.model.ExecutionContext;
import com.example.cae.nodeagent.domain.model.ExecutionResult;
import com.example.cae.nodeagent.infrastructure.process.ProcessRunner;
import com.example.cae.nodeagent.infrastructure.storage.ResultFileCollector;
import com.example.cae.nodeagent.infrastructure.support.CommandBuilder;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.Map;

@Component
public class CalculixExecutor extends AbstractSolverExecutor {
	private final CommandBuilder commandBuilder;
	private final ProcessRunner processRunner;
	private final ResultFileCollector resultFileCollector;
	private final TaskReportManager taskReportManager;

	public CalculixExecutor(CommandBuilder commandBuilder,
						 ProcessRunner processRunner,
						 ResultFileCollector resultFileCollector,
						 TaskReportManager taskReportManager) {
		this.commandBuilder = commandBuilder;
		this.processRunner = processRunner;
		this.resultFileCollector = resultFileCollector;
		this.taskReportManager = taskReportManager;
	}

	@Override
	public boolean supports(ExecutionContext context) {
		return context != null && "CALCULIX".equalsIgnoreCase(context.getSolverCode());
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
			throw new BizException("calculix execute failed with exit code " + exitCode);
		}
		List<File> resultFiles = resultFileCollector.collect(context);
		int duration = (int) ((System.currentTimeMillis() - start) / 1000);
		return ExecutionResult.success(duration, "calculix execute success", Map.of("exitCode", exitCode), resultFiles);
	}
}
