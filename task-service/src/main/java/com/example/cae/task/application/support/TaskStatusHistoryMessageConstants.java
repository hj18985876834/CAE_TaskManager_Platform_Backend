package com.example.cae.task.application.support;

public final class TaskStatusHistoryMessageConstants {
	public static final String VALIDATION_PASSED = "validation passed";
	public static final String TASK_PARAMETERS_UPDATED_REVALIDATION_REQUIRED =
			"task parameters updated, re-validation required";
	public static final String TASK_SUBMITTED = "task submitted";
	public static final String SCHEDULER_SELECTED_NODE = "scheduler selected node";
	public static final String TASK_DISPATCHED = "task dispatched";
	public static final String TASK_FINISHED = "task finished";
	public static final String ADMIN_RETRIED_TASK = "admin retried task";
	public static final String INPUT_ARCHIVE_REPLACED_REVALIDATION_REQUIRED =
			"input archive replaced, re-validation required";
	public static final String NODE_OFFLINE_TASK_TERMINATED_BY_SCHEDULER =
			"node offline, task terminated by scheduler";
	public static final String IGNORED_LATE_PREFIX = "ignored late ";
	public static final String IGNORED_LATE_DISPATCH_FAILED_PREFIX = "ignored late dispatch-failed(";
	public static final String MARK_FINISHED_ACTION = "mark-finished";
	public static final String MARK_FAILED_ACTION = "mark-failed";
	public static final String TERMINAL_CALLBACK_DEFAULT_ACTION = "terminal-callback";
	public static final String RESULT_SUMMARY_REPORT_ACTION = "result-summary-report";
	public static final String RESULT_FILE_REPORT_ACTION = "result-file-report";
	public static final String RESULT_REPORT_DEFAULT_ACTION = "result-report";
	public static final String REQUESTED_EQUALS = ", requested=";
	public static final String CURRENT_EQUALS = ", current=";
	public static final String PRIORITY_ADJUSTED_PREFIX = "priority adjusted: ";
	public static final String UNKNOWN = "UNKNOWN";

	private TaskStatusHistoryMessageConstants() {
	}
}
