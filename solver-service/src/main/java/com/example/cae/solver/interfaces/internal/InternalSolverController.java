package com.example.cae.solver.interfaces.internal;

import com.example.cae.common.response.Result;
import com.example.cae.solver.application.facade.SolverFacade;
import com.example.cae.solver.interfaces.response.SolverDetailResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/solvers")
public class InternalSolverController {
	private final SolverFacade solverFacade;

	public InternalSolverController(SolverFacade solverFacade) {
		this.solverFacade = solverFacade;
	}

	@GetMapping("/{solverId}")
	public Result<SolverDetailResponse> getSolverDetail(@PathVariable("solverId") Long solverId) {
		return Result.success(solverFacade.getSolverDetail(solverId));
	}
}
