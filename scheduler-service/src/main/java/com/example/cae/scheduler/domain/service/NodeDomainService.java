package com.example.cae.scheduler.domain.service;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.exception.BizException;
import com.example.cae.scheduler.domain.model.ComputeNode;
import com.example.cae.scheduler.interfaces.request.NodeAgentRegisterRequest;
import com.example.cae.scheduler.interfaces.request.NodeHeartbeatRequest;
import com.example.cae.scheduler.interfaces.request.NodeRegisterRequest;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class NodeDomainService {
	public void validateRegisterRequest(NodeRegisterRequest request) {
		if (request == null || isBlank(request.getNodeCode()) || isBlank(request.getNodeName()) || isBlank(request.getHost())) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "invalid register request");
		}
		if (request.getMaxConcurrency() == null || request.getMaxConcurrency() < 1) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "invalid maxConcurrency");
		}
		if (request.getPort() == null || request.getPort() < 1 || request.getPort() > 65535) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "invalid port");
		}
		validateSolverIds(request.getSolverIds());
	}

	public void validateAgentRegisterRequest(NodeAgentRegisterRequest request) {
		if (request == null || isBlank(request.getNodeCode()) || isBlank(request.getNodeName()) || isBlank(request.getHost())) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "invalid register request");
		}
		if (request.getMaxConcurrency() == null || request.getMaxConcurrency() < 1) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "invalid maxConcurrency");
		}
		if (request.getPort() == null || request.getPort() < 1 || request.getPort() > 65535) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "invalid port");
		}
		validateSolverItems(request.getSolvers());
	}

	public void validateHeartbeatRequest(NodeHeartbeatRequest request) {
		if (request == null || request.getNodeId() == null
				|| request.getCpuUsage() == null || request.getMemoryUsage() == null || request.getRunningCount() == null
				|| request.getRunningCount() < 0) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "invalid heartbeat request");
		}
	}

	public boolean canDispatch(ComputeNode node) {
		return node != null && node.canDispatch();
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	private void validateSolverIds(List<Long> solverIds) {
		if (solverIds == null || solverIds.isEmpty()) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "solverIds cannot be empty");
		}
		Set<Long> uniqueSolverIds = new HashSet<>();
		for (Long solverId : solverIds) {
			if (solverId == null || solverId < 1) {
				throw new BizException(ErrorCodeConstants.BAD_REQUEST, "invalid solverId");
			}
			if (!uniqueSolverIds.add(solverId)) {
				throw new BizException(ErrorCodeConstants.BAD_REQUEST, "duplicate solverId in register request");
			}
		}
	}

	private void validateSolverItems(List<NodeAgentRegisterRequest.SolverItem> solvers) {
		if (solvers == null || solvers.isEmpty()) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "solvers cannot be empty");
		}
		Set<Long> uniqueSolverIds = new HashSet<>();
		for (NodeAgentRegisterRequest.SolverItem solver : solvers) {
			if (solver == null || solver.getSolverId() == null || solver.getSolverId() < 1) {
				throw new BizException(ErrorCodeConstants.BAD_REQUEST, "invalid solverId");
			}
			if (isBlank(solver.getSolverVersion())) {
				throw new BizException(ErrorCodeConstants.BAD_REQUEST, "solverVersion cannot be blank");
			}
			if (!uniqueSolverIds.add(solver.getSolverId())) {
				throw new BizException(ErrorCodeConstants.BAD_REQUEST, "duplicate solverId in register request");
			}
		}
	}
}
