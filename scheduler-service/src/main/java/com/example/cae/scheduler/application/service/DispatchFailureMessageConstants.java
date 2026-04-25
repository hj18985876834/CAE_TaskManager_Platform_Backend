package com.example.cae.scheduler.application.service;

public final class DispatchFailureMessageConstants {
	public static final String NODE_AGENT_REJECTED_DISPATCH_REQUEST =
			"node-agent rejected dispatch request";
	public static final String NODE_AGENT_DISPATCH_REQUEST_FAILED =
			"node-agent dispatch request failed";
	public static final String DISPATCH_FAILED_REJECTED_RUNNING =
			"dispatch-failed rejected, task already running";
	public static final String DISPATCH_FAILURE_RELEASE_FAILED_PREFIX =
			"reservation release failed after dispatch-failed: ";
	public static final String DISPATCH_WATCHDOG_TIMEOUT_RUNTIME_MISSING =
			"dispatch watchdog timeout, node-agent runtime missing";

	private DispatchFailureMessageConstants() {
	}
}
