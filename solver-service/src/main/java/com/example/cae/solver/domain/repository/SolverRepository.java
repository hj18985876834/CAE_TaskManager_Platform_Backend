package com.example.cae.solver.domain.repository;

import com.example.cae.common.response.PageResult;
import com.example.cae.solver.domain.model.SolverDefinition;
import com.example.cae.solver.interfaces.request.SolverPageQueryRequest;

import java.util.Optional;

public interface SolverRepository {
	Optional<SolverDefinition> findById(Long solverId);

	Optional<SolverDefinition> findBySolverCode(String solverCode);

	void save(SolverDefinition solver);

	void update(SolverDefinition solver);

	PageResult<SolverDefinition> page(SolverPageQueryRequest request, long offset, long pageSize);

	long count(SolverPageQueryRequest request);
}

