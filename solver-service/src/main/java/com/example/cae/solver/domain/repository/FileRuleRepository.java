package com.example.cae.solver.domain.repository;

import com.example.cae.solver.domain.model.SolverProfileFileRule;

import java.util.List;
import java.util.Optional;

public interface FileRuleRepository {
	Optional<SolverProfileFileRule> findById(Long ruleId);

	void save(SolverProfileFileRule rule);

	void update(SolverProfileFileRule rule);

	void delete(Long ruleId);

	List<SolverProfileFileRule> listByProfileId(Long profileId);
}

