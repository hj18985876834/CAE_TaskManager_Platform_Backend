package com.example.cae.scheduler.interfaces.internal;

import com.example.cae.common.response.Result;
import com.example.cae.scheduler.application.service.NodeAppService;
import com.example.cae.scheduler.interfaces.request.NodeReservationRequest;
import com.example.cae.scheduler.interfaces.response.NodeReservationActionResponse;
import com.example.cae.scheduler.interfaces.response.NodeReservationAuditResponse;
import com.example.cae.scheduler.interfaces.response.NodeReservationReconcileResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/internal/nodes")
public class InternalNodeReservationController {
	private final NodeAppService nodeAppService;

	public InternalNodeReservationController(NodeAppService nodeAppService) {
		this.nodeAppService = nodeAppService;
	}

	@PostMapping("/{nodeId}/reserve")
	public Result<NodeReservationActionResponse> reserve(@PathVariable @Positive(message = "nodeId must be greater than 0") Long nodeId,
														 @Valid @RequestBody NodeReservationRequest request) {
		return Result.success(nodeAppService.reserveReservation(nodeId, request.getTaskId()));
	}

	@PostMapping("/{nodeId}/release-reservation")
	public Result<NodeReservationActionResponse> releaseReservation(@PathVariable @Positive(message = "nodeId must be greater than 0") Long nodeId,
																	@Valid @RequestBody NodeReservationRequest request) {
		return Result.success(nodeAppService.releaseReservation(nodeId, request.getTaskId()));
	}
	@PostMapping("/{nodeId}/reconcile-reservation")
	public Result<NodeReservationReconcileResponse> reconcileReservation(@PathVariable @Positive(message = "nodeId must be greater than 0") Long nodeId) {
		return Result.success(nodeAppService.reconcileReservation(nodeId));
	}

	@PostMapping("/{nodeId}/audit-reservation")
	public Result<NodeReservationAuditResponse> auditReservation(@PathVariable @Positive(message = "nodeId must be greater than 0") Long nodeId) {
		return Result.success(nodeAppService.auditReservation(nodeId));
	}
}
