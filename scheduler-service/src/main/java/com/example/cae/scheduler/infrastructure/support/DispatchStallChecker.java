package com.example.cae.scheduler.infrastructure.support;

import com.example.cae.common.dto.TaskBasicDTO;
import com.example.cae.common.enums.FailTypeEnum;
import com.example.cae.common.enums.TaskStatusEnum;
import com.example.cae.scheduler.application.manager.TaskScheduleManager;
import com.example.cae.scheduler.application.service.DispatchFailureMessageConstants;
import com.example.cae.scheduler.application.service.NodeAppService;
import com.example.cae.scheduler.domain.model.ComputeNode;
import com.example.cae.scheduler.domain.model.NodeReservation;
import com.example.cae.scheduler.domain.repository.NodeReservationRepository;
import com.example.cae.scheduler.infrastructure.client.NodeAgentClient;
import com.example.cae.scheduler.infrastructure.client.TaskClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
public class DispatchStallChecker {
	private static final Logger log = LoggerFactory.getLogger(DispatchStallChecker.class);
	private static final int NODE_HEARTBEAT_TIMEOUT_SECONDS = 30;

	private final NodeAppService nodeAppService;
	private final NodeReservationRepository nodeReservationRepository;
	private final TaskClient taskClient;
	private final NodeAgentClient nodeAgentClient;
	private final TaskScheduleManager taskScheduleManager;
	private final long dispatchStallTimeoutSeconds;

	public DispatchStallChecker(NodeAppService nodeAppService,
								NodeReservationRepository nodeReservationRepository,
								TaskClient taskClient,
								NodeAgentClient nodeAgentClient,
								TaskScheduleManager taskScheduleManager,
								@Value("${scheduler.dispatch-stall-timeout-seconds:30}") long dispatchStallTimeoutSeconds) {
		this.nodeAppService = nodeAppService;
		this.nodeReservationRepository = nodeReservationRepository;
		this.taskClient = taskClient;
		this.nodeAgentClient = nodeAgentClient;
		this.taskScheduleManager = taskScheduleManager;
		this.dispatchStallTimeoutSeconds = dispatchStallTimeoutSeconds;
	}

	public void recoverStalledDispatches() {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime stallThreshold = now.minusSeconds(Math.max(1L, dispatchStallTimeoutSeconds));
		LocalDateTime heartbeatThreshold = now.minusSeconds(NODE_HEARTBEAT_TIMEOUT_SECONDS);
		for (ComputeNode node : nodeAppService.listOnlineNodes()) {
			if (!isFreshOnlineNode(node, heartbeatThreshold)) {
				continue;
			}
			recoverStalledDispatchesOnNode(node, stallThreshold);
		}
	}

	private void recoverStalledDispatchesOnNode(ComputeNode node, LocalDateTime stallThreshold) {
		List<NodeReservation> reservations = nodeReservationRepository.listReservedByNodeId(node.getId()).stream()
				.filter(reservation -> isReservationStalled(reservation, stallThreshold))
				.toList();
		if (reservations.isEmpty()) {
			return;
		}
		Map<Long, TaskBasicDTO> taskBasics = taskClient.getTaskBasics(reservations.stream()
				.map(NodeReservation::getTaskId)
				.filter(taskId -> taskId != null)
				.distinct()
				.toList());
		for (NodeReservation reservation : reservations) {
			if (reservation == null || reservation.getTaskId() == null) {
				continue;
			}
			TaskBasicDTO taskBasic = taskBasics.get(reservation.getTaskId());
			if (!isRecoverableStalledDispatch(taskBasic, node.getId())) {
				continue;
			}
			probeAndRecover(reservation.getTaskId(), node.getId());
		}
	}

	private void probeAndRecover(Long taskId, Long nodeId) {
		try {
			if (nodeAgentClient.isTaskActive(nodeId, taskId)) {
				return;
			}
			taskScheduleManager.handleDispatchFailure(
					taskId,
					nodeId,
					FailTypeEnum.DISPATCH_ERROR.name(),
					DispatchFailureMessageConstants.DISPATCH_WATCHDOG_TIMEOUT_RUNTIME_MISSING,
					true
			);
			log.info("recovered stalled dispatch by requeueing task, taskId={}, nodeId={}, reason={}",
					taskId,
					nodeId,
					DispatchFailureMessageConstants.DISPATCH_WATCHDOG_TIMEOUT_RUNTIME_MISSING);
		} catch (Exception ex) {
			log.warn("failed to recover stalled dispatch, taskId={}, nodeId={}", taskId, nodeId, ex);
		}
	}

	private boolean isRecoverableStalledDispatch(TaskBasicDTO taskBasic, Long nodeId) {
		if (taskBasic == null || taskBasic.getStatus() == null || taskBasic.getStatus().isBlank()) {
			return false;
		}
		if (nodeId == null || !nodeId.equals(taskBasic.getNodeId())) {
			return false;
		}
		String status = taskBasic.getStatus().trim().toUpperCase();
		return TaskStatusEnum.SCHEDULED.name().equals(status)
				|| TaskStatusEnum.DISPATCHED.name().equals(status);
	}

	private boolean isReservationStalled(NodeReservation reservation, LocalDateTime stallThreshold) {
		if (reservation == null || reservation.getTaskId() == null || reservation.getNodeId() == null) {
			return false;
		}
		LocalDateTime effectiveTime = reservation.getUpdatedAt() == null ? reservation.getCreatedAt() : reservation.getUpdatedAt();
		return effectiveTime != null && !effectiveTime.isAfter(stallThreshold);
	}

	private boolean isFreshOnlineNode(ComputeNode node, LocalDateTime heartbeatThreshold) {
		return node != null
				&& node.getId() != null
				&& node.getLastHeartbeatTime() != null
				&& !node.getLastHeartbeatTime().isBefore(heartbeatThreshold);
	}
}
