package com.example.cae.scheduler.interfaces.internal;

import com.example.cae.common.response.Result;
import com.example.cae.scheduler.application.facade.NodeFacade;
import com.example.cae.scheduler.interfaces.request.NodeRegisterRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/scheduler/nodes")
public class NodeRegisterController {
	private final NodeFacade nodeFacade;

	public NodeRegisterController(NodeFacade nodeFacade) {
		this.nodeFacade = nodeFacade;
	}

	@PostMapping("/register")
	public Result<Void> register(@RequestBody NodeRegisterRequest request) {
		nodeFacade.registerNode(request);
		return Result.success();
	}
}

