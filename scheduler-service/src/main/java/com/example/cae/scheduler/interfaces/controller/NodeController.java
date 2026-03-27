package com.example.cae.scheduler.interfaces.controller;

import com.example.cae.common.response.PageResult;
import com.example.cae.common.response.Result;
import com.example.cae.scheduler.application.facade.NodeFacade;
import com.example.cae.scheduler.interfaces.request.NodePageQueryRequest;
import com.example.cae.scheduler.interfaces.request.NodeStatusUpdateRequest;
import com.example.cae.scheduler.interfaces.response.NodeDetailResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/nodes")
public class NodeController {
	private final NodeFacade nodeFacade;

	public NodeController(NodeFacade nodeFacade) {
		this.nodeFacade = nodeFacade;
	}

	@GetMapping
	public Result<PageResult<NodeDetailResponse>> pageNodes(NodePageQueryRequest request) {
		return Result.success(nodeFacade.pageNodes(request));
	}

	@GetMapping("/{nodeId}")
	public Result<NodeDetailResponse> getNodeDetail(@PathVariable Long nodeId) {
		return Result.success(nodeFacade.getNodeDetail(nodeId));
	}

	@PostMapping("/{nodeId}/status")
	public Result<Void> updateNodeStatus(@PathVariable Long nodeId, @RequestBody NodeStatusUpdateRequest request) {
		nodeFacade.updateNodeStatus(nodeId, request);
		return Result.success();
	}

	@GetMapping("/{nodeId}/solvers")
	public Result<List<Long>> listNodeSolvers(@PathVariable Long nodeId) {
		return Result.success(nodeFacade.listNodeSolvers(nodeId));
	}
}

