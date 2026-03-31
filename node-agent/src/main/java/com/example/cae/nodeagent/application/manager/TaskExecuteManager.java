package com.example.cae.nodeagent.application.manager;

import com.example.cae.nodeagent.domain.executor.SolverExecutor;
import com.example.cae.nodeagent.domain.model.ExecutionContext;
import com.example.cae.nodeagent.domain.model.ExecutionResult;
import com.example.cae.nodeagent.domain.service.ExecutorSelectDomainService;
import com.example.cae.nodeagent.infrastructure.storage.InputFilePrepareService;
import com.example.cae.nodeagent.infrastructure.storage.WorkDirManager;
import org.springframework.stereotype.Service;

@Service
public class TaskExecuteManager {
	private final WorkDirManager workDirManager;
	private final InputFilePrepareService inputFilePrepareService;
	private final ExecutorSelectDomainService executorSelectDomainService;
	private final TaskReportManager taskReportManager;

	public TaskExecuteManager(WorkDirManager workDirManager,
						 InputFilePrepareService inputFilePrepareService,
						 ExecutorSelectDomainService executorSelectDomainService,
						 TaskReportManager taskReportManager) {
		this.workDirManager = workDirManager;
		this.inputFilePrepareService = inputFilePrepareService;
		this.executorSelectDomainService = executorSelectDomainService;
		this.taskReportManager = taskReportManager;
	}

	public void execute(ExecutionContext context) {
		long start = System.currentTimeMillis();
		try {
			prepareWorkDir(context);
			prepareInputFiles(context);
			taskReportManager.reportRunning(context);
			SolverExecutor executor = selectExecutor(context);
			ExecutionResult result = executor.execute(context);
			taskReportManager.reportSuccess(context, result, start);
		} catch (Exception ex) {
			taskReportManager.reportFail(context, ex);
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
