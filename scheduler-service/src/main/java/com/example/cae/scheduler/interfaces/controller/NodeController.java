package com.example.cae.scheduler.interfaces.controller;

import com.example.cae.common.response.PageResult;
import com.example.cae.common.response.Result;
import com.example.cae.scheduler.application.facade.NodeFacade;
import com.example.cae.scheduler.interfaces.request.NodePageQueryRequest;
import com.example.cae.scheduler.interfaces.request.UpdateNodeSolverStatusRequest;
import com.example.cae.scheduler.interfaces.request.UpdateNodeStatusRequest;
import com.example.cae.scheduler.interfaces.response.NodeDetailResponse;
import com.example.cae.scheduler.interfaces.response.NodeListItemResponse;
import com.example.cae.scheduler.interfaces.response.NodeSolverResponse;
import com.example.cae.scheduler.interfaces.response.NodeSolverStatusResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/nodes")
public class NodeController {
	private final NodeFacade nodeFacade;

	public NodeController(NodeFacade nodeFacade) {
		this.nodeFacade = nodeFacade;
	}

	@GetMapping
	public Result<PageResult<NodeListItemResponse>> pageNodes(@Valid NodePageQueryRequest request) {
		return Result.success(nodeFacade.pageNodes(request));
	}

	@GetMapping("/{nodeId}")
	public Result<NodeDetailResponse> getNodeDetail(@PathVariable("nodeId") @Positive(message = "nodeId must be greater than 0") Long nodeId) {
		return Result.success(nodeFacade.getNodeDetail(nodeId));
	}

	@PostMapping("/{nodeId}/status")
	public Result<Void> updateNodeStatus(@PathVariable("nodeId") @Positive(message = "nodeId must be greater than 0") Long nodeId,
									 @Valid @RequestBody UpdateNodeStatusRequest request) {
		nodeFacade.updateNodeStatus(nodeId, request);
		return Result.success();
	}

	@PostMapping("/{nodeId}/solvers/{solverId}/status")
	public Result<NodeSolverStatusResponse> updateNodeSolverStatus(@PathVariable("nodeId") @Positive(message = "nodeId must be greater than 0") Long nodeId,
										  @PathVariable("solverId") @Positive(message = "solverId must be greater than 0") Long solverId,
										  @Valid @RequestBody UpdateNodeSolverStatusRequest request) {
		return Result.success(nodeFacade.updateNodeSolverStatus(nodeId, solverId, request));
	}

	@GetMapping("/{nodeId}/solvers")
	public Result<List<NodeSolverResponse>> listNodeSolvers(@PathVariable("nodeId") @Positive(message = "nodeId must be greater than 0") Long nodeId) {
		return Result.success(nodeFacade.listNodeSolvers(nodeId));
	}
}
