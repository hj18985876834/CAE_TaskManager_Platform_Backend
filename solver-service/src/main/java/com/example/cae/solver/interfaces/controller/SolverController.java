package com.example.cae.solver.interfaces.controller;

import com.example.cae.common.response.PageResult;
import com.example.cae.common.response.Result;
import com.example.cae.solver.application.facade.SolverFacade;
import com.example.cae.solver.interfaces.request.CreateSolverRequest;
import com.example.cae.solver.interfaces.request.SolverPageQueryRequest;
import com.example.cae.solver.interfaces.request.UpdateSolverRequest;
import com.example.cae.solver.interfaces.request.UpdateSolverStatusRequest;
import com.example.cae.solver.interfaces.response.ProfileListItemResponse;
import com.example.cae.solver.interfaces.response.SolverCreateResponse;
import com.example.cae.solver.interfaces.response.SolverDetailResponse;
import com.example.cae.solver.interfaces.response.SolverListItemResponse;
import com.example.cae.solver.interfaces.response.SolverTaskOptionResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/solvers")
public class SolverController {
	private final SolverFacade solverFacade;

	public SolverController(SolverFacade solverFacade) {
		this.solverFacade = solverFacade;
	}

	@GetMapping
	public Result<PageResult<SolverListItemResponse>> pageSolvers(SolverPageQueryRequest request) {
		return Result.success(solverFacade.pageSolvers(request));
	}

	@GetMapping("/{solverId}")
	public Result<SolverDetailResponse> getSolverDetail(@PathVariable("solverId") Long solverId) {
		return Result.success(solverFacade.getSolverDetail(solverId));
	}

	@PostMapping
	public Result<SolverCreateResponse> createSolver(@RequestBody CreateSolverRequest request) {
		return Result.success(solverFacade.createSolver(request));
	}

	@PutMapping("/{solverId}")
	public Result<Void> updateSolver(@PathVariable("solverId") Long solverId, @RequestBody UpdateSolverRequest request) {
		solverFacade.updateSolver(solverId, request);
		return Result.success();
	}

	@PutMapping("/{solverId}/status")
	public Result<Void> updateSolverStatus(@PathVariable("solverId") Long solverId, @RequestBody UpdateSolverStatusRequest request) {
		solverFacade.updateSolverStatus(solverId, request);
		return Result.success();
	}

	@PostMapping("/{solverId}/status")
	public Result<Void> updateSolverStatusPost(@PathVariable("solverId") Long solverId, @RequestBody UpdateSolverStatusRequest request) {
		solverFacade.updateSolverStatus(solverId, request);
		return Result.success();
	}

	@GetMapping("/{solverId}/profiles")
	public Result<List<ProfileListItemResponse>> getSolverProfiles(@PathVariable("solverId") Long solverId) {
		return Result.success(solverFacade.getSolverProfiles(solverId));
	}

	@GetMapping("/{solverId}/task-options")
	public Result<List<SolverTaskOptionResponse>> getSolverTaskOptions(@PathVariable("solverId") Long solverId) {
		return Result.success(solverFacade.getSolverTaskOptions(solverId));
	}
}
