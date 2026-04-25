package com.example.cae.scheduler.domain.repository;

import com.example.cae.scheduler.domain.model.NodeReservation;

import java.util.Optional;

public interface NodeReservationRepository {
	Optional<NodeReservation> findByNodeIdAndTaskIdForUpdate(Long nodeId, Long taskId);

	void save(NodeReservation reservation);

	void update(NodeReservation reservation);

	int countReservedByNodeId(Long nodeId);
}
