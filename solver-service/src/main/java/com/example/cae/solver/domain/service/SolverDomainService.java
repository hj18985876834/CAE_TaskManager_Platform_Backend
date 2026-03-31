package com.example.cae.solver.domain.service;

import com.example.cae.common.constant.ErrorCodeConstants;
import com.example.cae.common.exception.BizException;
import com.example.cae.solver.domain.repository.SolverRepository;
import org.springframework.stereotype.Service;

@Service
public class SolverDomainService {
	private final SolverRepository solverRepository;

	public SolverDomainService(SolverRepository solverRepository) {
		this.solverRepository = solverRepository;
	}

	public void checkSolverCodeUnique(String solverCode) {
		if (solverCode == null || solverCode.trim().isEmpty()) {
			throw new BizException(ErrorCodeConstants.BAD_REQUEST, "solverCode is empty");
		}
		if (solverRepository.findBySolverCode(solverCode).isPresent()) {
			throw new BizException(ErrorCodeConstants.SOLVER_CODE_ALREADY_EXISTS, "solverCode already exists");
		}
	}
}
