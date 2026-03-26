package com.example.cae.scheduler.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ScheduleLogWriter {
	private static final Logger log = LoggerFactory.getLogger(ScheduleLogWriter.class);

	public void info(String message) {
		log.info("[scheduler] {}", message);
	}

	public void warn(String message) {
		log.warn("[scheduler] {}", message);
	}
}
