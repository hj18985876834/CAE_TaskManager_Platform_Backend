package com.example.cae.scheduler.domain.strategy;

import com.example.cae.common.dto.TaskDTO;
import com.example.cae.scheduler.domain.model.ComputeNode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Component
public class FcfsLeastLoadStrategy implements ScheduleStrategy {
	private static final Comparator<ComputeNode> NODE_ORDER = Comparator
			.comparing(ComputeNode::getTotalLoad)
			.thenComparing(ComputeNode::getRunningCount, Comparator.nullsLast(Integer::compareTo))
			.thenComparing(ComputeNode::getCpuUsage, Comparator.nullsLast(BigDecimal::compareTo))
			.thenComparing(ComputeNode::getMemoryUsage, Comparator.nullsLast(BigDecimal::compareTo))
			.thenComparing(ComputeNode::getLastHeartbeatTime, Comparator.nullsLast(Comparator.reverseOrder()));

	@Override
	public ComputeNode selectNode(TaskDTO task, List<ComputeNode> nodes) {
		return orderNodes(task, nodes).stream().findFirst().orElse(null);
	}

	@Override
	public List<ComputeNode> orderNodes(TaskDTO task, List<ComputeNode> nodes) {
		if (nodes == null || nodes.isEmpty()) {
			return List.of();
		}
		return nodes.stream()
				.sorted(NODE_ORDER)
				.toList();
	}
}
