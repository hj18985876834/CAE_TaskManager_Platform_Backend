package com.example.cae.scheduler.domain.strategy;

import com.example.cae.scheduler.domain.model.ComputeNode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FcfsLeastLoadStrategyTest {
	private final FcfsLeastLoadStrategy strategy = new FcfsLeastLoadStrategy();

	@Test
	void selectNodeShouldPreferLatestHeartbeatWhenLoadIsEqual() {
		ComputeNode olderHeartbeatNode = buildNode(1L, LocalDateTime.of(2026, 4, 21, 10, 0));
		ComputeNode newerHeartbeatNode = buildNode(2L, LocalDateTime.of(2026, 4, 21, 10, 5));

		ComputeNode selected = strategy.selectNode(null, List.of(olderHeartbeatNode, newerHeartbeatNode));

		assertEquals(2L, selected.getId());
	}

	private ComputeNode buildNode(Long nodeId, LocalDateTime heartbeatTime) {
		ComputeNode node = new ComputeNode();
		node.setId(nodeId);
		node.setRunningCount(1);
		node.setReservedCount(0);
		node.setCpuUsage(new BigDecimal("30.0"));
		node.setMemoryUsage(new BigDecimal("40.0"));
		node.setLastHeartbeatTime(heartbeatTime);
		return node;
	}
}
