package com.example.cae.solver.interfaces.controller;

import com.example.cae.common.response.PageResult;
import com.example.cae.common.response.Result;
import com.example.cae.solver.application.facade.SolverFacade;
import com.example.cae.solver.interfaces.request.CreateSolverRequest;
import com.example.cae.solver.interfaces.request.SolverPageQueryRequest;
import com.example.cae.solver.interfaces.request.UpdateSolverRequest;
import com.example.cae.solver.interfaces.request.UpdateSolverStatusRequest;
import com.example.cae.solver.interfaces.response.SolverProfileOptionResponse;
import com.example.cae.solver.interfaces.response.SolverCreateResponse;
import com.example.cae.solver.interfaces.response.SolverDetailResponse;
import com.example.cae.solver.interfaces.response.SolverListItemResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/solvers")
public class SolverController {
	private final SolverFacade solverFacade;

	public SolverController(SolverFacade solverFacade) {
		this.solverFacade = solverFacade;
	}

	@GetMapping
	public Result<PageResult<SolverListItemResponse>> pageSolvers(@Valid SolverPageQueryRequest request) {
		return Result.success(solverFacade.pageSolvers(request));
	}

	@GetMapping("/{solverId}")
	public Result<SolverDetailResponse> getSolverDetail(@PathVariable("solverId") @Positive(message = "solverId must be greater than 0") Long solverId) {
		return Result.success(solverFacade.getSolverDetail(solverId));
	}

	@PostMapping
	public Result<SolverCreateResponse> createSolver(@Valid @RequestBody CreateSolverRequest request) {
		return Result.success(solverFacade.createSolver(request));
	}

	@PutMapping("/{solverId}")
	public Result<Void> updateSolver(@PathVariable("solverId") @Positive(message = "solverId must be greater than 0") Long solverId,
									 @Valid @RequestBody UpdateSolverRequest request) {
		solverFacade.updateSolver(solverId, request);
		return Result.success();
	}

	@PostMapping("/{solverId}/status")
	public Result<Void> updateSolverStatusPost(@PathVariable("solverId") @Positive(message = "solverId must be greater than 0") Long solverId,
											   @Valid @RequestBody UpdateSolverStatusRequest request) {
		solverFacade.updateSolverStatus(solverId, request);
		return Result.success();
	}

	@GetMapping("/{solverId}/profiles")
	public Result<List<SolverProfileOptionResponse>> getSolverProfiles(@PathVariable("solverId") @Positive(message = "solverId must be greater than 0") Long solverId) {
		return Result.success(solverFacade.getSolverProfileOptions(solverId));
	}
}
