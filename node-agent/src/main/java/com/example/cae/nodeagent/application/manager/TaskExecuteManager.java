package com.example.cae.nodeagent.application.manager;

import com.example.cae.nodeagent.domain.executor.SolverExecutor;
import com.example.cae.nodeagent.domain.model.ExecutionContext;
import com.example.cae.nodeagent.domain.model.ExecutionResult;
import com.example.cae.nodeagent.domain.service.ExecutorSelectDomainService;
import com.example.cae.nodeagent.infrastructure.process.ProcessCanceledException;
import com.example.cae.nodeagent.infrastructure.storage.InputFilePrepareService;
import com.example.cae.nodeagent.infrastructure.storage.WorkDirManager;
import org.springframework.stereotype.Service;

@Service
public class TaskExecuteManager {
	private final WorkDirManager workDirManager;
	private final InputFilePrepareService inputFilePrepareService;
	private final ExecutorSelectDomainService executorSelectDomainService;
	private final TaskReportManager taskReportManager;
	private final TaskRuntimeRegistry taskRuntimeRegistry;

	public TaskExecuteManager(WorkDirManager workDirManager,
						 InputFilePrepareService inputFilePrepareService,
						 ExecutorSelectDomainService executorSelectDomainService,
						 TaskReportManager taskReportManager,
						 TaskRuntimeRegistry taskRuntimeRegistry) {
		this.workDirManager = workDirManager;
		this.inputFilePrepareService = inputFilePrepareService;
		this.executorSelectDomainService = executorSelectDomainService;
		this.taskReportManager = taskReportManager;
		this.taskRuntimeRegistry = taskRuntimeRegistry;
	}

	public void execute(ExecutionContext context) {
		long start = System.currentTimeMillis();
		try {
			taskRuntimeRegistry.attachWorker(context.getTaskId(), Thread.currentThread());
			if (taskRuntimeRegistry.isCancelRequested(context.getTaskId())) {
				taskReportManager.reportCanceled(context, taskRuntimeRegistry.getCancelReason(context.getTaskId()));
				return;
			}
			prepareWorkDir(context);
			prepareInputFiles(context);
			taskReportManager.reportRunning(context);
			SolverExecutor executor = selectExecutor(context);
			ExecutionResult result = executor.execute(context);
			if (Boolean.TRUE.equals(result.getSuccess())) {
				taskReportManager.reportSuccess(context, result, start);
			} else {
				taskReportManager.reportFail(context, new RuntimeException(result.getSummaryText()));
			}
		} catch (Exception ex) {
			if (taskRuntimeRegistry.isCancelRequested(context.getTaskId()) || ex instanceof ProcessCanceledException) {
				taskReportManager.reportCanceled(context, taskRuntimeRegistry.getCancelReason(context.getTaskId()));
			} else {
				taskReportManager.reportFail(context, ex);
			}
		} finally {
			taskReportManager.completeTask(context.getTaskId());
		}
	}

	public void prepareWorkDir(ExecutionContext context) {
		workDirManager.prepareTaskDirs(context);
	}

	public void prepareInputFiles(ExecutionContext context) {
		inputFilePrepareService.prepare(context);
	}

	public SolverExecutor selectExecutor(ExecutionContext context) {
		return executorSelectDomainService.selectExecutor(context);
	}
}
