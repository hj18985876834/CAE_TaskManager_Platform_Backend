package com.example.cae.solver.domain.service;

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
			throw new BizException(400, "solverCode is empty");
		}
		if (solverRepository.findBySolverCode(solverCode).isPresent()) {
			throw new BizException(400, "solverCode already exists");
		}
	}
}
