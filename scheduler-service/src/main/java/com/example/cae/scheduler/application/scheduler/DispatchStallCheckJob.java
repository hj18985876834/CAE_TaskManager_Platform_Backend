package com.example.cae.scheduler.application.scheduler;

import com.example.cae.scheduler.infrastructure.support.DispatchStallChecker;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DispatchStallCheckJob {
	private final DispatchStallChecker dispatchStallChecker;

	public DispatchStallCheckJob(DispatchStallChecker dispatchStallChecker) {
		this.dispatchStallChecker = dispatchStallChecker;
	}

	@Scheduled(fixedDelayString = "${scheduler.dispatch-stall-check-interval-ms:15000}")
	public void run() {
		dispatchStallChecker.recoverStalledDispatches();
	}
}
