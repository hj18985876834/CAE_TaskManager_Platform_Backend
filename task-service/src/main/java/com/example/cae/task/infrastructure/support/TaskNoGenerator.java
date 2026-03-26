package com.example.cae.task.infrastructure.support;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class TaskNoGenerator {
	public String generateTaskNo() {
		String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
		int rand = ThreadLocalRandom.current().nextInt(100, 1000);
		return "TASK" + ts + rand;
	}
}

