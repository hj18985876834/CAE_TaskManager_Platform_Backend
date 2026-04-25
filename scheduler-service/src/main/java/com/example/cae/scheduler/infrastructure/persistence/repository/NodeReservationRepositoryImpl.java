package com.example.cae.scheduler.infrastructure.persistence.repository;

import com.example.cae.scheduler.domain.model.NodeReservation;
import com.example.cae.scheduler.domain.repository.NodeReservationRepository;
import com.example.cae.scheduler.infrastructure.persistence.entity.NodeReservationPO;
import com.example.cae.scheduler.infrastructure.persistence.mapper.NodeReservationMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class NodeReservationRepositoryImpl implements NodeReservationRepository {
	private final NodeReservationMapper nodeReservationMapper;

	public NodeReservationRepositoryImpl(NodeReservationMapper nodeReservationMapper) {
		this.nodeReservationMapper = nodeReservationMapper;
	}

	@Override
	public Optional<NodeReservation> findByNodeIdAndTaskIdForUpdate(Long nodeId, Long taskId) {
		return Optional.ofNullable(nodeReservationMapper.selectByNodeIdAndTaskIdForUpdate(nodeId, taskId))
				.map(this::fromPO);
	}

	@Override
	public void save(NodeReservation reservation) {
		NodeReservationPO po = toPO(reservation);
		nodeReservationMapper.insert(po);
		reservation.setId(po.getId());
	}

	@Override
	public void update(NodeReservation reservation) {
		nodeReservationMapper.updateById(toPO(reservation));
	}

	@Override
	public int countReservedByNodeId(Long nodeId) {
		return nodeReservationMapper.countReservedByNodeId(nodeId);
	}

	@Override
	public List<NodeReservation> listReservedByNodeId(Long nodeId) {
		return nodeReservationMapper.selectReservedByNodeId(nodeId).stream()
				.map(this::fromPO)
				.toList();
	}

	private NodeReservation fromPO(NodeReservationPO po) {
		NodeReservation reservation = new NodeReservation();
		reservation.setId(po.getId());
		reservation.setNodeId(po.getNodeId());
		reservation.setTaskId(po.getTaskId());
		reservation.setStatus(po.getStatus());
		reservation.setCreatedAt(po.getCreatedAt());
		reservation.setUpdatedAt(po.getUpdatedAt());
		reservation.setReleasedAt(po.getReleasedAt());
		return reservation;
	}

	private NodeReservationPO toPO(NodeReservation reservation) {
		NodeReservationPO po = new NodeReservationPO();
		po.setId(reservation.getId());
		po.setNodeId(reservation.getNodeId());
		po.setTaskId(reservation.getTaskId());
		po.setStatus(reservation.getStatus());
		po.setCreatedAt(reservation.getCreatedAt());
		po.setUpdatedAt(reservation.getUpdatedAt());
		po.setReleasedAt(reservation.getReleasedAt());
		return po;
	}
}
