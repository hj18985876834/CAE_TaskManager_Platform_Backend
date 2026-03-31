package com.example.cae.scheduler.interfaces.controller;

import com.example.cae.common.constant.HeaderConstants;
import com.example.cae.common.response.Result;
import com.example.cae.scheduler.application.facade.NodeFacade;
import com.example.cae.scheduler.interfaces.request.NodeAgentRegisterRequest;
import com.example.cae.scheduler.interfaces.request.NodeHeartbeatRequest;
import com.example.cae.scheduler.interfaces.response.NodeAgentRegisterResponse;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/node-agent")
public class NodeAgentController {
	private final NodeFacade nodeFacade;

	public NodeAgentController(NodeFacade nodeFacade) {
		this.nodeFacade = nodeFacade;
	}

	@PostMapping("/register")
	public Result<NodeAgentRegisterResponse> register(@Valid @RequestBody NodeAgentRegisterRequest request) {
		Long nodeId = nodeFacade.registerNodeFromAgent(request);
		NodeAgentRegisterResponse response = new NodeAgentRegisterResponse();
		response.setNodeId(nodeId);
		response.setNodeToken(nodeId == null ? null : nodeFacade.getNodeToken(nodeId));
		return Result.success(response);
	}

	@PostMapping("/heartbeat")
	public Result<Void> heartbeat(@Valid @RequestBody NodeHeartbeatRequest request,
								  @RequestHeader(value = HeaderConstants.X_NODE_TOKEN, required = true) String nodeToken) {
		nodeFacade.heartbeat(request, nodeToken);
		return Result.success();
	}
}
