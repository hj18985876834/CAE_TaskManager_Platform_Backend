package com.example.cae.scheduler.interfaces.internal;

import com.example.cae.common.response.Result;
import com.example.cae.scheduler.application.service.NodeAppService;
import com.example.cae.scheduler.interfaces.request.NodeReservationRequest;
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
	public Result<Void> reserve(@PathVariable @Positive(message = "nodeId must be greater than 0") Long nodeId,
								@Valid @RequestBody NodeReservationRequest request) {
		nodeAppService.reserveReservation(nodeId, request.getTaskId());
		return Result.success();
	}

	@PostMapping("/{nodeId}/release-reservation")
	public Result<Void> releaseReservation(@PathVariable @Positive(message = "nodeId must be greater than 0") Long nodeId,
										   @Valid @RequestBody NodeReservationRequest request) {
		nodeAppService.releaseReservation(nodeId, request.getTaskId());
		return Result.success();
	}
}
