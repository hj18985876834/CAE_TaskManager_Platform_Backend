package com.example.cae.nodeagent.application.manager;

import com.example.cae.nodeagent.application.service.TaskReportAppService;
import com.example.cae.nodeagent.domain.model.ExecutionContext;
import com.example.cae.nodeagent.domain.model.ExecutionResult;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class TaskReportManager {
	private final TaskReportAppService taskReportAppService;
	private final AtomicInteger logSeq = new AtomicInteger(1);

	public TaskReportManager(TaskReportAppService taskReportAppService) {
		this.taskReportAppService = taskReportAppService;
	}

	public void reportRunning(ExecutionContext context) {
		taskReportAppService.reportStatus(context.getTaskId(), "RUNNING", "node-agent start execute");
	}

	public void pushLog(Long taskId, Integer seqNo, String line) {
		int actualSeqNo = seqNo == null ? logSeq.getAndIncrement() : seqNo;
		taskReportAppService.reportLog(taskId, actualSeqNo, line);
	}

	public void reportSuccess(ExecutionContext context, ExecutionResult result, long startMillis) {
		int duration = result.getDurationSeconds() == null
				? (int) ((System.currentTimeMillis() - startMillis) / 1000)
				: result.getDurationSeconds();
		result.setDurationSeconds(duration);
		taskReportAppService.reportResultSummary(context.getTaskId(), result);
		if (result.getResultFiles() != null) {
			for (File file : result.getResultFiles()) {
				taskReportAppService.reportResultFile(context.getTaskId(), file);
			}
		}
		taskReportAppService.markFinished(context.getTaskId());
	}

	public void reportFail(ExecutionContext context, Exception ex) {
		taskReportAppService.markFailed(context.getTaskId(), "RUNTIME_ERROR", ex.getMessage());
	}
}

