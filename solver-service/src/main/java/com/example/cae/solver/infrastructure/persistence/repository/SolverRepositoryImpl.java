package com.example.cae.solver.infrastructure.persistence.repository;

import com.example.cae.common.response.PageResult;
import com.example.cae.solver.application.assembler.SolverAssembler;
import com.example.cae.solver.domain.model.SolverDefinition;
import com.example.cae.solver.domain.repository.SolverRepository;
import com.example.cae.solver.infrastructure.persistence.mapper.SolverDefinitionMapper;
import com.example.cae.solver.interfaces.request.SolverPageQueryRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class SolverRepositoryImpl implements SolverRepository {
	private final SolverDefinitionMapper solverDefinitionMapper;

	public SolverRepositoryImpl(SolverDefinitionMapper solverDefinitionMapper) {
		this.solverDefinitionMapper = solverDefinitionMapper;
	}

	@Override
	public Optional<SolverDefinition> findById(Long solverId) {
		return Optional.ofNullable(solverDefinitionMapper.selectById(solverId)).map(SolverAssembler::fromPO);
	}

	@Override
	public Optional<SolverDefinition> findBySolverCode(String solverCode) {
		return Optional.ofNullable(solverDefinitionMapper.selectBySolverCode(solverCode)).map(SolverAssembler::fromPO);
	}

	@Override
	public void save(SolverDefinition solver) {
		var po = SolverAssembler.toPO(solver);
		solverDefinitionMapper.insert(po);
		solver.setId(po.getId());
	}

	@Override
	public void update(SolverDefinition solver) {
		solverDefinitionMapper.updateById(SolverAssembler.toPO(solver));
	}

	@Override
	public PageResult<SolverDefinition> page(SolverPageQueryRequest request, long offset, long pageSize) {
		List<SolverDefinition> records = solverDefinitionMapper.selectPage(request, offset, pageSize)
				.stream()
				.map(SolverAssembler::fromPO)
				.toList();
		long pageNum = request == null || request.getPageNum() == null ? 1L : request.getPageNum();
		return PageResult.of(count(request), pageNum, pageSize, records);
	}

	@Override
	public long count(SolverPageQueryRequest request) {
		return solverDefinitionMapper.count(request);
	}
}
