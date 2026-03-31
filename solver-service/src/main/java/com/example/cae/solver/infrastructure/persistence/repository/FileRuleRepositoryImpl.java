package com.example.cae.solver.infrastructure.persistence.repository;

import com.example.cae.solver.application.assembler.FileRuleAssembler;
import com.example.cae.solver.domain.model.SolverProfileFileRule;
import com.example.cae.solver.domain.repository.FileRuleRepository;
import com.example.cae.solver.infrastructure.persistence.mapper.SolverProfileFileRuleMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class FileRuleRepositoryImpl implements FileRuleRepository {
	private final SolverProfileFileRuleMapper solverProfileFileRuleMapper;

	public FileRuleRepositoryImpl(SolverProfileFileRuleMapper solverProfileFileRuleMapper) {
		this.solverProfileFileRuleMapper = solverProfileFileRuleMapper;
	}

	@Override
	public Optional<SolverProfileFileRule> findById(Long ruleId) {
		return Optional.ofNullable(solverProfileFileRuleMapper.selectById(ruleId)).map(FileRuleAssembler::fromPO);
	}

	@Override
	public void save(SolverProfileFileRule rule) {
		var po = FileRuleAssembler.toPO(rule);
		solverProfileFileRuleMapper.insert(po);
		rule.setId(po.getId());
	}

	@Override
	public void update(SolverProfileFileRule rule) {
		solverProfileFileRuleMapper.updateById(FileRuleAssembler.toPO(rule));
	}

	@Override
	public void delete(Long ruleId) {
		solverProfileFileRuleMapper.deleteById(ruleId);
	}

	@Override
	public List<SolverProfileFileRule> listByProfileId(Long profileId) {
		return solverProfileFileRuleMapper.selectByProfileId(profileId).stream().map(FileRuleAssembler::fromPO).toList();
	}
}
