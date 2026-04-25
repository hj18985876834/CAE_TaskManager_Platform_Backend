package com.example.cae.solver.application.facade;

import com.example.cae.common.response.PageResult;
import com.example.cae.solver.application.service.SolverAppService;
import com.example.cae.solver.interfaces.request.CreateSolverRequest;
import com.example.cae.solver.interfaces.request.SolverPageQueryRequest;
import com.example.cae.solver.interfaces.request.UpdateSolverRequest;
import com.example.cae.solver.interfaces.request.UpdateSolverStatusRequest;
import com.example.cae.solver.interfaces.response.SolverProfileOptionResponse;
import com.example.cae.solver.interfaces.response.SolverCreateResponse;
import com.example.cae.solver.interfaces.response.SolverDetailResponse;
import com.example.cae.solver.interfaces.response.SolverListItemResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SolverFacade {
	private final SolverAppService solverAppService;

	public SolverFacade(SolverAppService solverAppService) {
		this.solverAppService = solverAppService;
	}

	public PageResult<SolverListItemResponse> pageSolvers(SolverPageQueryRequest request) {
		return solverAppService.pageSolvers(request);
	}

	public SolverDetailResponse getSolverDetail(Long solverId) {
		return solverAppService.getSolverDetail(solverId);
	}

	public SolverCreateResponse createSolver(CreateSolverRequest request) {
		return solverAppService.createSolver(request);
	}

	public void updateSolver(Long solverId, UpdateSolverRequest request) {
		solverAppService.updateSolver(solverId, request);
	}

	public void updateSolverStatus(Long solverId, UpdateSolverStatusRequest request) {
		solverAppService.updateSolverStatus(solverId, request);
	}

	public List<SolverProfileOptionResponse> getSolverProfileOptions(Long solverId) {
		return solverAppService.getSolverProfileOptions(solverId);
	}
}
