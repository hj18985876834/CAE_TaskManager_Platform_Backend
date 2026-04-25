package com.example.cae.scheduler.interfaces.response;

public class NodeReservationReconcileResponse {
	private Long nodeId;
	private Integer beforeReservedCount;
	private Integer actualReservedCount;
	private Integer afterReservedCount;
	private Boolean changed;

	public Long getNodeId() {
		return nodeId;
	}

	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

	public Integer getBeforeReservedCount() {
		return beforeReservedCount;
	}

	public void setBeforeReservedCount(Integer beforeReservedCount) {
		this.beforeReservedCount = beforeReservedCount;
	}

	public Integer getActualReservedCount() {
		return actualReservedCount;
	}

	public void setActualReservedCount(Integer actualReservedCount) {
		this.actualReservedCount = actualReservedCount;
	}

	public Integer getAfterReservedCount() {
		return afterReservedCount;
	}

	public void setAfterReservedCount(Integer afterReservedCount) {
		this.afterReservedCount = afterReservedCount;
	}

	public Boolean getChanged() {
		return changed;
	}

	public void setChanged(Boolean changed) {
		this.changed = changed;
	}
}
