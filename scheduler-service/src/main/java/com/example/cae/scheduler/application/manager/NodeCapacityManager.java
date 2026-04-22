package com.example.cae.scheduler.application.manager;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.exception.BizException;
import com.example.cae.scheduler.domain.model.ComputeNode;
import com.example.cae.scheduler.domain.model.NodeReservation;
import com.example.cae.scheduler.domain.repository.ComputeNodeRepository;
import com.example.cae.scheduler.domain.repository.NodeReservationRepository;
import com.example.cae.scheduler.interfaces.response.NodeReservationActionResponse;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class NodeCapacityManager {
	private final ComputeNodeRepository computeNodeRepository;
	private final NodeReservationRepository nodeReservationRepository;

	public NodeCapacityManager(ComputeNodeRepository computeNodeRepository,
							   NodeReservationRepository nodeReservationRepository) {
		this.computeNodeRepository = computeNodeRepository;
		this.nodeReservationRepository = nodeReservationRepository;
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
