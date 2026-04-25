package com.example.cae.scheduler.application.manager;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.dto.TaskBasicDTO;
import com.example.cae.common.enums.TaskStatusEnum;
import com.example.cae.common.exception.BizException;
import com.example.cae.scheduler.domain.model.ComputeNode;
import com.example.cae.scheduler.domain.model.NodeReservation;
import com.example.cae.scheduler.domain.repository.ComputeNodeRepository;
import com.example.cae.scheduler.domain.repository.NodeReservationRepository;
import com.example.cae.scheduler.infrastructure.client.TaskClient;
import com.example.cae.scheduler.interfaces.response.NodeReservationActionResponse;
import com.example.cae.scheduler.interfaces.response.NodeReservationAuditResponse;
import com.example.cae.scheduler.interfaces.response.NodeReservationReconcileResponse;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class NodeCapacityManager {
	private static final Set<String> RESERVATION_COMPATIBLE_TASK_STATUSES = Set.of(
			TaskStatusEnum.SCHEDULED.name(),
			TaskStatusEnum.DISPATCHED.name()
	);
	private final ComputeNodeRepository computeNodeRepository;
	private final NodeReservationRepository nodeReservationRepository;
	private final TaskClient taskClient;

	public NodeCapacityManager(ComputeNodeRepository computeNodeRepository,
							   NodeReservationRepository nodeReservationRepository,
							   TaskClient taskClient) {
		this.computeNodeRepository = computeNodeRepository;
		this.nodeReservationRepository = nodeReservationRepository;
		this.taskClient = taskClient;
	}

	@Transactional
	public NodeReservationActionResponse reserve(Long nodeId, Long taskId) {
		validateRequest(nodeId, taskId);
		ComputeNode node = computeNodeRepository.findByIdForUpdate(nodeId)
				.orElseThrow(() -> new BizException(ErrorCodeConstants.NODE_NOT_FOUND, "node not found"));
		NodeReservation reservation = nodeReservationRepository.findByNodeIdAndTaskIdForUpdate(nodeId, taskId).orElse(null);
		if (reservation != null && reservation.isReserved()) {
			return buildResponse(taskId, nodeId, reservation.getStatus(), safeReservedCount(node));
		}
		if (!node.reserveSlot()) {
			throw new BizException(ErrorCodeConstants.NO_AVAILABLE_NODE, "no available node");
		}
		computeNodeRepository.update(node);
		if (reservation == null) {
			NodeReservation newReservation = new NodeReservation();
			newReservation.setNodeId(nodeId);
			newReservation.setTaskId(taskId);
			newReservation.markReserved();
			nodeReservationRepository.save(newReservation);
			return buildResponse(taskId, nodeId, newReservation.getStatus(), safeReservedCount(node));
		}
		reservation.markReserved();
		nodeReservationRepository.update(reservation);
		return buildResponse(taskId, nodeId, reservation.getStatus(), safeReservedCount(node));
	}

	@Transactional
	public NodeReservationActionResponse release(Long nodeId, Long taskId) {
		validateRequest(nodeId, taskId);
		ComputeNode node = computeNodeRepository.findByIdForUpdate(nodeId)
				.orElseThrow(() -> new BizException(ErrorCodeConstants.NODE_NOT_FOUND, "node not found"));
		NodeReservation reservation = nodeReservationRepository.findByNodeIdAndTaskIdForUpdate(nodeId, taskId).orElse(null);
		if (reservation == null) {
			throw new BizException(ErrorCodeConstants.CONFLICT, "node reservation does not exist");
		}
		if (!reservation.isReserved()) {
			return buildResponse(taskId, nodeId, reservation.getStatus(), safeReservedCount(node));
		}
		node.releaseReservation();
		computeNodeRepository.update(node);
		reservation.markReleased();
		nodeReservationRepository.update(reservation);
		return buildResponse(taskId, nodeId, reservation.getStatus(), safeReservedCount(node));
	}

	@Transactional
	public NodeReservationReconcileResponse reconcileReservedCount(Long nodeId) {
		if (nodeId == null) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "nodeId is required");
		}
		ComputeNode node = computeNodeRepository.findByIdForUpdate(nodeId)
				.orElseThrow(() -> new BizException(ErrorCodeConstants.NODE_NOT_FOUND, "node not found"));
		int before = safeReservedCount(node);
		int actual = nodeReservationRepository.countReservedByNodeId(nodeId);
		if (before != actual) {
			node.setReservedCount(actual);
			computeNodeRepository.update(node);
		}
		NodeReservationReconcileResponse response = new NodeReservationReconcileResponse();
		response.setNodeId(nodeId);
		response.setBeforeReservedCount(before);
		response.setActualReservedCount(actual);
		response.setAfterReservedCount(actual);
		response.setChanged(before != actual);
		return response;
	}

	public NodeReservationAuditResponse auditReservations(Long nodeId) {
		if (nodeId == null) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "nodeId is required");
		}
		ComputeNode node = computeNodeRepository.findById(nodeId)
				.orElseThrow(() -> new BizException(ErrorCodeConstants.NODE_NOT_FOUND, "node not found"));
		List<NodeReservation> reservations = nodeReservationRepository.listReservedByNodeId(nodeId);
		Map<Long, TaskBasicDTO> taskBasics = taskClient.getTaskBasics(reservations.stream()
				.map(NodeReservation::getTaskId)
				.filter(taskId -> taskId != null)
				.distinct()
				.collect(Collectors.toList()));

		List<NodeReservationAuditResponse.NodeReservationAuditItemResponse> issues = reservations.stream()
				.map(reservation -> buildAuditIssue(reservation, taskBasics.get(reservation.getTaskId())))
				.filter(issue -> issue != null)
				.toList();

		NodeReservationAuditResponse response = new NodeReservationAuditResponse();
		response.setNodeId(nodeId);
		response.setReservedCount(safeReservedCount(node));
		response.setReservedDetailCount(reservations.size());
		response.setConsistent(safeReservedCount(node) == reservations.size() && issues.isEmpty());
		response.setIssues(issues);
		return response;
	}

	private NodeReservationAuditResponse.NodeReservationAuditItemResponse buildAuditIssue(NodeReservation reservation,
																			 TaskBasicDTO taskBasic) {
		String issueType = resolveReservationIssueType(reservation, taskBasic);
		if (issueType == null) {
			return null;
		}
		NodeReservationAuditResponse.NodeReservationAuditItemResponse issue = new NodeReservationAuditResponse.NodeReservationAuditItemResponse();
		issue.setReservationId(reservation.getId());
		issue.setTaskId(reservation.getTaskId());
		issue.setReservationNodeId(reservation.getNodeId());
		issue.setReservationStatus(reservation.getStatus());
		issue.setTaskStatus(taskBasic == null ? null : taskBasic.getStatus());
		issue.setTaskNodeId(taskBasic == null ? null : taskBasic.getNodeId());
		issue.setIssueType(issueType);
		issue.setMessage(buildReservationIssueMessage(issueType));
		issue.setUpdatedAt(reservation.getUpdatedAt());
		return issue;
	}

	private String resolveReservationIssueType(NodeReservation reservation, TaskBasicDTO taskBasic) {
		if (taskBasic == null || taskBasic.getTaskId() == null) {
			return "TASK_NOT_FOUND";
		}
		if (!RESERVATION_COMPATIBLE_TASK_STATUSES.contains(taskBasic.getStatus())) {
			return "TASK_STATUS_NOT_RESERVABLE";
		}
		if (!Objects.equals(reservation.getNodeId(), taskBasic.getNodeId())) {
			return "TASK_NODE_MISMATCH";
		}
		return null;
	}

	private String buildReservationIssueMessage(String issueType) {
		return switch (issueType) {
			case "TASK_NOT_FOUND" -> "reserved node_reservation points to a missing task";
			case "TASK_STATUS_NOT_RESERVABLE" -> "reserved node_reservation points to a task that is no longer SCHEDULED or DISPATCHED";
			case "TASK_NODE_MISMATCH" -> "reserved node_reservation nodeId does not match task bound nodeId";
			default -> "reserved node_reservation is inconsistent with task state";
		};
	}
	private void validateRequest(Long nodeId, Long taskId) {
		if (nodeId == null || taskId == null) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "nodeId and taskId are required");
		}
	}

	private NodeReservationActionResponse buildResponse(Long taskId,
														Long nodeId,
														String reservationStatus,
														Integer reservedCount) {
		NodeReservationActionResponse response = new NodeReservationActionResponse();
		response.setTaskId(taskId);
		response.setNodeId(nodeId);
		response.setReservationStatus(reservationStatus);
		response.setReservedCount(reservedCount);
		return response;
	}

	private Integer safeReservedCount(ComputeNode node) {
		return node == null || node.getReservedCount() == null ? 0 : node.getReservedCount();
	}
}
