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

	private ScheduleAuditMessageConstants() {
	}
}
