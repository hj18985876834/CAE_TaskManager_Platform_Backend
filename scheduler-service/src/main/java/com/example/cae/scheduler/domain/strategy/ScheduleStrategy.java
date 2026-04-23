package com.example.cae.scheduler.domain.strategy;

import com.example.cae.common.dto.TaskDTO;
import com.example.cae.scheduler.domain.model.ComputeNode;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public interface ScheduleStrategy {
	ComputeNode selectNode(TaskDTO task, List<ComputeNode> nodes);

	default List<ComputeNode> orderNodes(TaskDTO task, List<ComputeNode> nodes) {
		if (nodes == null || nodes.isEmpty()) {
			return List.of();
		}
		ComputeNode selected = selectNode(task, nodes);
		if (selected == null) {
			return List.of();
		}
		return Stream.concat(
				Stream.of(selected),
				nodes.stream().filter(node -> !sameNode(node, selected))
		).toList();
	}

	private static boolean sameNode(ComputeNode left, ComputeNode right) {
		if (left == null || right == null) {
			return false;
		}
		if (left.getId() != null || right.getId() != null) {
			return Objects.equals(left.getId(), right.getId());
		}
		return left == right;
	}
}
