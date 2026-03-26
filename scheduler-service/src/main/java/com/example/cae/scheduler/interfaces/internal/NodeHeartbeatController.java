package com.example.cae.scheduler.interfaces.internal;

import com.example.cae.common.response.Result;
import com.example.cae.scheduler.application.facade.NodeFacade;
import com.example.cae.scheduler.interfaces.request.NodeHeartbeatRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/scheduler/nodes")
public class NodeHeartbeatController {
	private final NodeFacade nodeFacade;

	public NodeHeartbeatController(NodeFacade nodeFacade) {
		this.nodeFacade = nodeFacade;
	}

	@PostMapping("/heartbeat")
	public Result<Void> heartbeat(@RequestBody NodeHeartbeatRequest request) {
		nodeFacade.heartbeat(request);
		return Result.success();
	}
}

