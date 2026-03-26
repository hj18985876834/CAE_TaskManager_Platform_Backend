package com.example.cae.scheduler.domain.strategy;

import com.example.cae.common.dto.TaskDTO;
import com.example.cae.scheduler.domain.model.ComputeNode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Component
public class FcfsLeastLoadStrategy implements ScheduleStrategy {
	@Override
	public ComputeNode selectNode(TaskDTO task, List<ComputeNode> nodes) {
		if (nodes == null || nodes.isEmpty()) {
			return null;
		}
		return nodes.stream()
				.min(Comparator
						.comparing(ComputeNode::getRunningCount, Comparator.nullsLast(Integer::compareTo))
						.thenComparing(ComputeNode::getCpuUsage, Comparator.nullsLast(BigDecimal::compareTo))
						.thenComparing(ComputeNode::getMemoryUsage, Comparator.nullsLast(BigDecimal::compareTo)))
				.orElse(null);
	}
}

