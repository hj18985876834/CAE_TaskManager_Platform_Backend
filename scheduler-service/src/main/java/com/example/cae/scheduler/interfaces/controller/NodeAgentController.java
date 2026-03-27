package com.example.cae.scheduler.interfaces.controller;

import com.example.cae.common.response.Result;
import com.example.cae.scheduler.application.facade.NodeFacade;
import com.example.cae.scheduler.interfaces.request.NodeAgentRegisterRequest;
import com.example.cae.scheduler.interfaces.request.NodeHeartbeatRequest;
import com.example.cae.scheduler.interfaces.request.NodeRegisterRequest;
import com.example.cae.scheduler.interfaces.response.NodeAgentRegisterResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/node-agent")
public class NodeAgentController {
	private final NodeFacade nodeFacade;

	public NodeAgentController(NodeFacade nodeFacade) {
		this.nodeFacade = nodeFacade;
	}

	@PostMapping("/register")
	public Result<NodeAgentRegisterResponse> register(@RequestBody NodeAgentRegisterRequest request) {
		NodeRegisterRequest registerRequest = new NodeRegisterRequest();
		registerRequest.setNodeCode(request == null ? null : request.getNodeCode());
		registerRequest.setNodeName(request == null ? null : request.getNodeName());
		registerRequest.setHost(request == null ? null : request.getHost());
		registerRequest.setPort(request == null ? null : request.getPort());
		registerRequest.setMaxConcurrency(request == null ? null : request.getMaxConcurrency());
		registerRequest.setSolverIds(extractSolverIds(request));

		Long nodeId = nodeFacade.registerNode(registerRequest);
		NodeAgentRegisterResponse response = new NodeAgentRegisterResponse();
		response.setNodeId(nodeId);
		response.setNodeToken(nodeId == null ? null : "node-token-" + nodeId);
		return Result.success(response);
	}

	@PostMapping("/heartbeat")
	public Result<Void> heartbeat(@RequestBody NodeHeartbeatRequest request) {
		nodeFacade.heartbeat(request);
		return Result.success();
	}

	private List<Long> extractSolverIds(NodeAgentRegisterRequest request) {
		if (request == null || request.getSolvers() == null || request.getSolvers().isEmpty()) {
			return List.of();
		}
		return request.getSolvers().stream()
				.map(NodeAgentRegisterRequest.SolverItem::getSolverId)
				.filter(id -> id != null)
				.toList();
	}
}