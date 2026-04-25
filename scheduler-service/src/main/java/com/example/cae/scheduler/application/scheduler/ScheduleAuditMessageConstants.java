package com.example.cae.scheduler.application.scheduler;

public final class ScheduleAuditMessageConstants {
	public static final String SCHEDULE_CLAIM_REJECTED = "schedule claim rejected";
	public static final String SCHEDULE_CLAIM_REJECTED_ALREADY_CLAIMED_BY_ANOTHER_SCHEDULER =
			"schedule claim rejected, task already claimed by another scheduler";
	public static final String SCHEDULE_CLAIM_REJECTED_ALREADY_DISPATCHED_BY_ANOTHER_SCHEDULER =
			"schedule claim rejected, task already dispatched by another scheduler";
	public static final String SCHEDULE_CLAIM_REJECTED_ALREADY_RUNNING =
			"schedule claim rejected, task already running";
	public static final String SCHEDULE_CLAIM_REJECTED_ALREADY_FINISHED =
			"schedule claim rejected, task already finished";
	public static final String MARK_DISPATCHED_CONFIRM_FAILED_DEFAULT_REASON =
			"mark-dispatched confirm failed";
	public static final String NODE_ACCEPTED_MARK_DISPATCHED_CONFIRM_FAILED_PREFIX =
			"node accepted task, mark-dispatched confirm failed; wait for node RUNNING or dispatch-failed callback: ";
	public static final String TASK_DISPATCHED = "task dispatched";
	public static final String TASK_DISPATCHED_ALREADY_RUNNING = "task dispatched, already running";
	public static final String MARK_DISPATCHED_ACK_RECOVERED_ALREADY_RUNNING =
			"mark-dispatched ACK recovered, task already running";
	public static final String MARK_DISPATCHED_ACK_RECOVERED_ALREADY_FINISHED =
			"mark-dispatched ACK recovered, task already finished";
	public static final String MARK_DISPATCHED_ACK_RECOVERED_ALREADY_FAILED =
			"mark-dispatched ACK recovered, task already failed";
	public static final String MARK_DISPATCHED_ACK_RECOVERED_ALREADY_CANCELED =
			"mark-dispatched ACK recovered, task already canceled";

	private ScheduleAuditMessageConstants() {
	}
}
