package com.example.cae.scheduler.application.assembler;

import com.example.cae.scheduler.domain.model.ScheduleRecord;
import com.example.cae.scheduler.infrastructure.persistence.entity.ScheduleRecordPO;
import com.example.cae.scheduler.interfaces.response.ScheduleRecordResponse;

public final class ScheduleAssembler {
	private ScheduleAssembler() {
	}

	public static ScheduleRecordResponse toResponse(ScheduleRecord record) {
		ScheduleRecordResponse response = new ScheduleRecordResponse();
		response.setId(record.getId());
		response.setTaskId(record.getTaskId());
		response.setNodeId(record.getNodeId());
		response.setStrategyName(record.getStrategyName());
		response.setScheduleStatus(record.getScheduleStatus());
		response.setScheduleMessage(record.getScheduleMessage());
		response.setCreatedAt(record.getCreatedAt());
		return response;
	}

	public static ScheduleRecord newRecord(Long taskId, Long nodeId, String strategyName, String status, String message) {
		ScheduleRecord record = new ScheduleRecord();
		record.setTaskId(taskId);
		record.setNodeId(nodeId);
		record.setStrategyName(strategyName);
		record.setScheduleStatus(status);
		record.setScheduleMessage(message);
		return record;
	}

	public static ScheduleRecord fromPO(ScheduleRecordPO po) {
		ScheduleRecord record = new ScheduleRecord();
		record.setId(po.getId());
		record.setTaskId(po.getTaskId());
		record.setNodeId(po.getNodeId());
		record.setStrategyName(po.getStrategyName());
		record.setScheduleStatus(po.getScheduleStatus());
		record.setScheduleMessage(po.getScheduleMessage());
		record.setCreatedAt(po.getCreatedAt());
		return record;
	}

	public static ScheduleRecordPO toPO(ScheduleRecord record) {
		ScheduleRecordPO po = new ScheduleRecordPO();
		po.setId(record.getId());
		po.setTaskId(record.getTaskId());
		po.setNodeId(record.getNodeId());
		po.setStrategyName(record.getStrategyName());
		po.setScheduleStatus(record.getScheduleStatus());
		po.setScheduleMessage(record.getScheduleMessage());
		po.setCreatedAt(record.getCreatedAt());
		return po;
	}
}
